// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.ui.cloneDialog

import com.gitee.api.*
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.ui.GEAccountsDetailsProvider
import com.gitee.exceptions.GiteeAuthenticationException
import com.gitee.exceptions.GiteeMissingTokenException
import com.gitee.i18n.GiteeBundle
import com.gitee.util.*
import com.intellij.collaboration.async.disposingMainScope
import com.intellij.collaboration.auth.ui.CompactAccountsPanelFactory
import com.intellij.collaboration.ui.CollaborationToolsUIUtil
import com.intellij.collaboration.util.CollectionDelta
import com.intellij.dvcs.repo.ClonePathProvider
import com.intellij.dvcs.ui.CloneDvcsValidationUtils
import com.intellij.dvcs.ui.DvcsBundle.message
import com.intellij.dvcs.ui.FilePathDocumentChildPathHandle
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vcs.CheckoutProvider
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.*
import com.intellij.ui.components.JBList
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.cloneDialog.AccountMenuItem.Action
import com.intellij.util.ui.cloneDialog.VcsCloneDialogUiSpec
import git4idea.GitUtil
import git4idea.checkout.GitCheckoutProvider
import git4idea.commands.Git
import git4idea.remote.GitRememberedInputs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import org.jetbrains.annotations.Nls
import java.awt.event.ActionEvent
import java.nio.file.Paths
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener
import kotlin.properties.Delegates

internal abstract class GECloneDialogExtensionComponentBase(
  private val project: Project,
  private val modalityState: ModalityState,
  private val authenticationManager: GiteeAuthenticationManager,
  private val executorManager: GiteeApiRequestExecutorManager
) : VcsCloneDialogExtensionComponent() {

  private val LOG = GiteeUtil.LOG

  private val githeeGitHelper: GiteeGitHelper = GiteeGitHelper.getInstance()

  private val cs = disposingMainScope() + modalityState.asContextElement()

  // UI
  private val wrapper: Wrapper = Wrapper()
  private val repositoriesPanel: DialogPanel
  private val repositoryList: JBList<GERepositoryListItem>

  private val searchField: SearchTextField
  private val directoryField = TextFieldWithBrowseButton().apply {
    val fcd = FileChooserDescriptorFactory.createSingleFolderDescriptor()
    fcd.isShowFileSystemRoots = true
    fcd.isHideIgnored = false
    addBrowseFolderListener(
      message("clone.destination.directory.browser.title"),
      message("clone.destination.directory.browser.description"),
      project,
      fcd
    )
  }
  private val cloneDirectoryChildHandle = FilePathDocumentChildPathHandle.install(
    directoryField.textField.document, ClonePathProvider.defaultParentDirectoryPath(project, GitRememberedInputs.getInstance())
  )

  // state
  private val loader = GECloneDialogRepositoryListLoaderImpl(executorManager)
  private var inLoginState = false
  private var selectedUrl by Delegates.observable<String?>(null) { _, _, _ -> onSelectedUrlChanged() }

  protected val content: JComponent get() = wrapper.targetComponent

  private val accountListModel: ListModel<GiteeAccount> = createAccountsModel()

  init {
    repositoryList = JBList(loader.listModel).apply {
      cellRenderer = GERepositoryListCellRenderer(ErrorHandler()) { getAccounts() }
      isFocusable = false
      selectionModel = loader.listSelectionModel
    }.also {
      val mouseAdapter = GERepositoryMouseAdapter(it)
      it.addMouseListener(mouseAdapter)
      it.addMouseMotionListener(mouseAdapter)
      it.addListSelectionListener { evt ->
        if (evt.valueIsAdjusting) return@addListSelectionListener
        updateSelectedUrl()
      }
    }

    //TODO: fix jumping selection in the presence of filter
    loader.addLoadingStateListener {
      repositoryList.setPaintBusy(loader.loading)
    }

    searchField = SearchTextField(false).also {
      it.addDocumentListener(object : DocumentAdapter() {
        override fun textChanged(e: DocumentEvent) = updateSelectedUrl()
      })
      createFocusFilterFieldAction(it)
    }

    CollaborationToolsUIUtil.attachSearch(repositoryList, searchField) {
      when (it) {
        is GERepositoryListItem.Repo -> it.repo.fullName
        is GERepositoryListItem.Error -> ""
      }
    }

    @Suppress("LeakingThis")
    val parentDisposable: Disposable = this
    Disposer.register(parentDisposable, loader)

    val accountDetailsProvider = GEAccountsDetailsProvider(cs, authenticationManager.accountManager)

    val accountsPanel = CompactAccountsPanelFactory(accountListModel)
      .create(accountDetailsProvider, VcsCloneDialogUiSpec.Components.avatarSize, AccountsPopupConfig())

    repositoriesPanel = panel {
      row {
        cell(searchField.textEditor)
          .resizableColumn()
          .align(Align.FILL)
        cell(JSeparator(JSeparator.VERTICAL))
          .align(AlignY.FILL)
        cell(accountsPanel)
          .align(AlignY.FILL)
      }
      row {
        scrollCell(repositoryList)
          .resizableColumn()
          .align(Align.FILL)
      }.resizableRow()

      row(GiteeBundle.message("clone.dialog.directory.field")) {
        cell(directoryField)
          .align(AlignX.FILL)
          .validationOnApply {
            CloneDvcsValidationUtils.checkDirectory(it.text, it.textField)
          }
      }
    }
    repositoriesPanel.border = JBEmptyBorder(UIUtil.getRegularPanelInsets())

    setupAccountsListeners()
  }

  protected abstract fun isAccountHandled(account: GiteeAccount): Boolean

  protected fun getAccounts(): Collection<GiteeAccount> = accountListModel.itemsSet

  protected abstract fun createLoginPanel(account: GiteeAccount?, cancelHandler: () -> Unit): JComponent

  private fun setupAccountsListeners() {
    accountListModel.addListDataListener(object : ListDataListener {

      private var currentList by Delegates.observable(emptySet<GiteeAccount>()) { _, oldValue, newValue ->
        val delta = CollectionDelta(oldValue, newValue)
        for (account in delta.removedItems) {
          loader.clear(account)
        }
        for (account in delta.newItems) {
          loader.loadRepositories(account)
        }

        if (newValue.isEmpty()) {
          switchToLogin(null)
        }
        else {
          switchToRepositories()
        }
        dialogStateListener.onListItemChanged()
      }

      init {
        currentList = accountListModel.itemsSet
      }

      override fun intervalAdded(e: ListDataEvent) {
        currentList = accountListModel.itemsSet
      }

      override fun intervalRemoved(e: ListDataEvent) {
        currentList = accountListModel.itemsSet
      }

      override fun contentsChanged(e: ListDataEvent) {
        for (i in e.index0..e.index1) {
          val account = accountListModel.getElementAt(i)
          loader.clear(account)
          loader.loadRepositories(account)
        }
        switchToRepositories()
        dialogStateListener.onListItemChanged()
      }
    })
  }

  protected fun switchToLogin(account: GiteeAccount?) {
    wrapper.setContent(createLoginPanel(account) { switchToRepositories() })
    wrapper.repaint()
    inLoginState = true
    updateSelectedUrl()
  }

  private fun switchToRepositories() {
    wrapper.setContent(repositoriesPanel)
    wrapper.repaint()
    inLoginState = false
    updateSelectedUrl()
  }

  override fun getView() = wrapper

  override fun doValidateAll(): List<ValidationInfo> =
    (wrapper.targetComponent as? DialogPanel)?.validationsOnApply?.values?.flatten()?.mapNotNull {
      it.validate()
    } ?: emptyList()

  override fun doClone(checkoutListener: CheckoutProvider.Listener) {
    val parent = Paths.get(directoryField.text).toAbsolutePath().parent
    val destinationValidation = CloneDvcsValidationUtils.createDestination(parent.toString())
    if (destinationValidation != null) {
      LOG.error("Unable to create destination directory", destinationValidation.message)
      GiteeNotifications.showError(project,
                                    GiteeNotificationIdsHolder.CLONE_UNABLE_TO_CREATE_DESTINATION_DIR,
                                    GiteeBundle.message("clone.dialog.clone.failed"),
                                    GiteeBundle.message("clone.error.unable.to.create.dest.dir"))
      return
    }

    val lfs = LocalFileSystem.getInstance()
    var destinationParent = lfs.findFileByIoFile(parent.toFile())
    if (destinationParent == null) {
      destinationParent = lfs.refreshAndFindFileByIoFile(parent.toFile())
    }
    if (destinationParent == null) {
      LOG.error("Clone Failed. Destination doesn't exist")
      GiteeNotifications.showError(project,
                                    GiteeNotificationIdsHolder.CLONE_UNABLE_TO_FIND_DESTINATION,
                                    GiteeBundle.message("clone.dialog.clone.failed"),
                                    GiteeBundle.message("clone.error.unable.to.find.dest"))
      return
    }
    val directoryName = Paths.get(directoryField.text).fileName.toString()
    val parentDirectory = parent.toAbsolutePath().toString()

    GitCheckoutProvider.clone(project, Git.getInstance(), checkoutListener, destinationParent, selectedUrl, directoryName, parentDirectory)
  }

  override fun onComponentSelected() {
    dialogStateListener.onOkActionNameChanged(GiteeBundle.message("clone.button"))
    updateSelectedUrl()

    val focusManager = IdeFocusManager.getInstance(project)
    getPreferredFocusedComponent()?.let { focusManager.requestFocus(it, true) }
  }

  override fun getPreferredFocusedComponent(): JComponent? {
    return searchField
  }

  private fun updateSelectedUrl() {
    repositoryList.emptyText.clear()
    if (inLoginState) {
      selectedUrl = null
      return
    }
    val githubRepoPath = getGiteeRepoPath(searchField.text)
    if (githubRepoPath != null) {
      selectedUrl = githeeGitHelper.getRemoteUrl(githubRepoPath.serverPath,
                                                 githubRepoPath.repositoryPath.owner,
                                                 githubRepoPath.repositoryPath.repository)
      repositoryList.emptyText.appendText(GiteeBundle.message("clone.dialog.text", selectedUrl!!))
      return
    }
    val selectedValue = repositoryList.selectedValue
    if (selectedValue is GERepositoryListItem.Repo) {
      selectedUrl = githeeGitHelper.getRemoteUrl(selectedValue.account.server,
                                                 selectedValue.repo.userName,
                                                 selectedValue.repo.name)
      return
    }
    selectedUrl = null
  }


  private fun getGiteeRepoPath(searchText: String): GERepositoryCoordinates? {
    val url = searchText
      .trim()
      .removePrefix("git clone")
      .removeSuffix(".git")
      .trim()

    try {
      var serverPath = GiteeServerPath.from(url)
      serverPath = GiteeServerPath.from(serverPath.toUrl().removeSuffix(serverPath.suffix ?: ""))

      val githubFullPath = GiteeUrlUtil.getUserAndRepositoryFromRemoteUrl(url) ?: return null
      return GERepositoryCoordinates(serverPath, githubFullPath)
    }
    catch (e: Throwable) {
      return null
    }
  }

  private fun onSelectedUrlChanged() {
    val urlSelected = selectedUrl != null
    dialogStateListener.onOkActionEnabled(urlSelected)
    if (urlSelected) {
      val path = StringUtil.trimEnd(ClonePathProvider.relativeDirectoryPathForVcsUrl(project, selectedUrl!!), GitUtil.DOT_GIT)
      cloneDirectoryChildHandle.trySetChildPath(path)
    }
  }

  private inner class AccountsPopupConfig : CompactAccountsPanelFactory.PopupConfig<GiteeAccount> {
    override val avatarSize: Int = VcsCloneDialogUiSpec.Components.popupMenuAvatarSize

    override fun createActions(): Collection<Action> = createAccountMenuLoginActions(null)
  }

  protected abstract fun createAccountMenuLoginActions(account: GiteeAccount?): Collection<Action>

  private fun createFocusFilterFieldAction(searchField: SearchTextField) {
    val action = DumbAwareAction.create {
      val focusManager = IdeFocusManager.getInstance(project)
      if (focusManager.getFocusedDescendantFor(repositoriesPanel) != null) {
        focusManager.requestFocus(searchField, true)
      }
    }
    val shortcuts = KeymapUtil.getActiveKeymapShortcuts(IdeActions.ACTION_FIND)
    action.registerCustomShortcutSet(shortcuts, repositoriesPanel, this)
  }

  private fun createAccountsModel(): ListModel<GiteeAccount> {
    val accountsState = authenticationManager.accountManager.accountsState
    val model = CollectionListModel(accountsState.value.keys.filter(::isAccountHandled))

    cs.launch(Dispatchers.Main.immediate) {
      val prev = accountsState.value.filterKeys(::isAccountHandled)

      accountsState.collect {
        val new = it.filterKeys(::isAccountHandled)

        new.forEach { (acc, token) ->
          if (!prev.containsKey(acc)) {
            model.add(acc)
          }
          else if (prev[acc] != token) {
            model.contentsChanged(acc)
          }
        }

        prev.forEach { (acc, _) ->
          if (!new.containsKey(acc)) {
            model.remove(acc)
          }
        }
      }
    }
    return model
  }

  private inner class ErrorHandler : GERepositoryListCellRenderer.ErrorHandler {

    override fun getPresentableText(error: Throwable): @Nls String = when (error) {
      is GiteeMissingTokenException -> GiteeBundle.message("account.token.missing")
      is GiteeAuthenticationException -> GiteeBundle.message("credentials.invalid.auth.data", "")
      else -> GiteeBundle.message("clone.error.load.repositories")
    }

    override fun getAction(account: GiteeAccount, error: Throwable) = when (error) {
      is GiteeAuthenticationException -> object : AbstractAction(GiteeBundle.message("accounts.relogin")) {
        override fun actionPerformed(e: ActionEvent?) {
          switchToLogin(account)
        }
      }
      else -> object : AbstractAction(GiteeBundle.message("retry.link")) {
        override fun actionPerformed(e: ActionEvent?) {
          loader.clear(account)
          loader.loadRepositories(account)
        }
      }
    }
  }

  companion object {
    internal val <E> ListModel<E>.items
      get() = Iterable {
        object : Iterator<E> {
          private var idx = -1

          override fun hasNext(): Boolean = idx < size - 1

          override fun next(): E {
            idx++
            return getElementAt(idx)
          }
        }
      }

    internal val <E> ListModel<E>.itemsSet
      get() = items.toSet()
  }
}