// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.ui.cloneDialog

import com.gitee.GiteeBundle
import com.gitee.api.*
import com.gitee.api.data.GiteeAuthenticatedUser
import com.gitee.api.data.GiteeRepo
import com.gitee.api.data.request.Affiliation
import com.gitee.api.data.request.GiteeRequestPagination
import com.gitee.api.util.GiteeApiPagesLoader
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.AccountRemovedListener
import com.gitee.authentication.accounts.AccountTokenChangedListener
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.accounts.GiteeAccountInformationProvider
import com.gitee.exceptions.GiteeMissingTokenException
import com.gitee.icons.GiteeIcons
import com.gitee.pullrequest.avatars.CachingGiteeAvatarIconsProvider
import com.gitee.util.*
import com.intellij.dvcs.repo.ClonePathProvider
import com.intellij.dvcs.ui.CloneDvcsValidationUtils
import com.intellij.dvcs.ui.DvcsBundle.getString
import com.intellij.dvcs.ui.SelectChildTextFieldWithBrowseButton
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.DialogPanel
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
import com.intellij.ui.layout.panel
import com.intellij.util.IconUtil
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.progress.ProgressVisibilityManager
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JBValue
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.cloneDialog.*
import com.intellij.util.ui.cloneDialog.AccountMenuItem.Account
import com.intellij.util.ui.cloneDialog.AccountMenuItem.Action
import git4idea.GitUtil
import git4idea.checkout.GitCheckoutProvider
import git4idea.commands.Git
import git4idea.remote.GitRememberedInputs
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.file.Paths
import javax.swing.*
import javax.swing.event.DocumentEvent
import kotlin.properties.Delegates

internal abstract class BaseCloneDialogExtensionComponent(
  private val project: Project,
  private val authenticationManager: GiteeAuthenticationManager,
  private val executorManager: GiteeApiRequestExecutorManager,
  private val accountInformationProvider: GiteeAccountInformationProvider,
  private val avatarLoader: CachingGiteeUserAvatarLoader,
  private val imageResizer: GiteeImageResizer
) : VcsCloneDialogExtensionComponent(),
  AccountRemovedListener,
  AccountTokenChangedListener {

  private val LOG = GiteeUtil.LOG

  private val progressManager: ProgressVisibilityManager
  private val giteeGitHelper: GiteeGitHelper = GiteeGitHelper.getInstance()

  // UI
  private val defaultAvatar = resizeIcon(GiteeIcons.DefaultAvatar, VcsCloneDialogUiSpec.Components.avatarSize)
  private val defaultPopupAvatar = resizeIcon(GiteeIcons.DefaultAvatar, VcsCloneDialogUiSpec.Components.popupMenuAvatarSize)
  private val avatarSizeUiInt = JBValue.UIInteger("GHCloneDialogExtensionComponent.popupAvatarSize",
                                                  VcsCloneDialogUiSpec.Components.popupMenuAvatarSize)


  private val wrapper: Wrapper = Wrapper()
  private val repositoriesPanel: DialogPanel
  private val repositoryList: JBList<GiteeRepositoryListItem>

  private val popupMenuMouseAdapter = object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent?) = showPopupMenu()
  }

  private val accountsPanel: JPanel = JPanel(FlowLayout(FlowLayout.LEADING, JBUI.scale(1), 0)).apply {
    addMouseListener(popupMenuMouseAdapter)
  }

  private val searchField: SearchTextField
  private val directoryField = SelectChildTextFieldWithBrowseButton(
    ClonePathProvider.defaultParentDirectoryPath(project, GitRememberedInputs.getInstance())).apply {
    val fcd = FileChooserDescriptorFactory.createSingleFolderDescriptor()
    fcd.isShowFileSystemRoots = true
    fcd.isHideIgnored = false
    addBrowseFolderListener(getString("clone.destination.directory.browser.title"),
                            getString("clone.destination.directory.browser.description"),
                            project,
                            fcd)
  }

  // state
  private val userDetailsByAccount = hashMapOf<GiteeAccount, GiteeAuthenticatedUser>()
  private val repositoriesByAccount = hashMapOf<GiteeAccount, LinkedHashSet<GiteeRepo>>()
  private val errorsByAccount = hashMapOf<GiteeAccount, GiteeRepositoryListItem.Error>()
  private val originListModel = CollectionListModel<GiteeRepositoryListItem>()
  private var inLoginState = false
  private var selectedUrl by Delegates.observable<String?>(null) { _, _, _ -> onSelectedUrlChanged() }

  // popup menu
  private val accountComponents = hashMapOf<GiteeAccount, JLabel>()
  private val avatarsByAccount = hashMapOf<GiteeAccount, Icon>()

  protected val content: JComponent get() = wrapper.targetComponent

  init {
    val listWithSearchBundle = ListWithSearchComponent(originListModel, GiteeRepositoryListCellRenderer { getAccounts() })

    repositoryList = listWithSearchBundle.list
    val mouseAdapter = GiteeRepositoryMouseAdapter(repositoryList)
    repositoryList.addMouseListener(mouseAdapter)
    repositoryList.addMouseMotionListener(mouseAdapter)
    repositoryList.addListSelectionListener {
      if (it.valueIsAdjusting) return@addListSelectionListener
      updateSelectedUrl()
    }

    searchField = listWithSearchBundle.searchField
    searchField.addDocumentListener(object : DocumentAdapter() {
      override fun textChanged(e: DocumentEvent) = updateSelectedUrl()
    })
    createFocusFilterFieldAction(searchField)

    progressManager = object : ProgressVisibilityManager() {
      override fun setProgressVisible(visible: Boolean) = repositoryList.setPaintBusy(visible)

      override fun getModalityState() = ModalityState.any()
    }

    Disposer.register(this, progressManager)

    repositoriesPanel = panel {
      row {
        cell(isFullWidth = true) {
          searchField.textEditor(pushX, growX)
          JSeparator(JSeparator.VERTICAL)(growY).withLargeLeftGap()
          accountsPanel().withLargeLeftGap()
        }
      }
      row {
        ScrollPaneFactory.createScrollPane(repositoryList)(push, grow)
      }
      row(GiteeBundle.message("clone.dialog.directory.field")) {
        directoryField(growX, pushX)
      }
    }
    repositoriesPanel.border = JBEmptyBorder(UIUtil.getRegularPanelInsets())
  }

  protected abstract fun getAccounts(): Collection<GiteeAccount>

  protected abstract fun createLoginPanel(account: GiteeAccount?, cancelHandler: () -> Unit): JComponent

  fun setup() {
    val accounts = getAccounts()
    if (accounts.isNotEmpty()) {
      switchToRepositories()
      accounts.forEach(::addAccount)
    }
    else {
      switchToLogin(null)
    }
  }

  override fun accountRemoved(removedAccount: GiteeAccount) {
    removeAccount(removedAccount)
    dialogStateListener.onListItemChanged()
  }

  override fun tokenChanged(account: GiteeAccount) {
    if (repositoriesByAccount[account] != null) return

    dialogStateListener.onListItemChanged()
    addAccount(account)
    switchToRepositories()
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

  private fun addAccount(account: GiteeAccount) {
    repositoriesByAccount.remove(account)

    val label = accountComponents.getOrPut(account) {
      JLabel().apply {
        icon = defaultAvatar
        toolTipText = account.name
        isOpaque = false
        addMouseListener(popupMenuMouseAdapter)
      }
    }
    accountsPanel.add(label)

    try {
      val executor = executorManager.getExecutor(account)
      loadUserDetails(account, executor)
      loadRepositories(account, executor)
    }
    catch (e: GiteeMissingTokenException) {
      errorsByAccount[account] = GiteeRepositoryListItem.Error(account,
                                                            GiteeBundle.message("account.token.missing"),
                                                            GiteeBundle.message("login.link"),
                                                            Runnable { switchToLogin(account) })
      refillRepositories()
    }
  }

  private fun removeAccount(account: GiteeAccount) {
    repositoriesByAccount.remove(account)
    accountComponents.remove(account).let {
      accountsPanel.remove(it)
      accountsPanel.revalidate()
      accountsPanel.repaint()
    }
    refillRepositories()
    if (getAccounts().isEmpty()) switchToLogin(null)
  }

  private fun loadUserDetails(account: GiteeAccount,
                              executor: GiteeApiRequestExecutor.WithTokensAuth) {
    progressManager.run(object : Task.Backgroundable(project, "Not Visible") {
      lateinit var user: GiteeAuthenticatedUser
      lateinit var iconProvider: CachingGiteeAvatarIconsProvider

      override fun run(indicator: ProgressIndicator) {
        user = accountInformationProvider.getInformation(executor, indicator, account)
        iconProvider = CachingGiteeAvatarIconsProvider
          .Factory(avatarLoader, imageResizer, executor)
          .create(avatarSizeUiInt, accountsPanel)
      }

      override fun onSuccess() {
        userDetailsByAccount[account] = user
        val avatar = iconProvider.getIcon(user.avatarUrl)
        avatarsByAccount[account] = avatar
        accountComponents[account]?.icon = resizeIcon(avatar, VcsCloneDialogUiSpec.Components.avatarSize)
        refillRepositories()
      }

      override fun onThrowable(error: Throwable) {
        LOG.error(error)
        errorsByAccount[account] = GiteeRepositoryListItem.Error(account,
                                                              GiteeBundle.message("clone.error.load.repositories"),
                                                              GiteeBundle.message("retry.link"),
                                                              Runnable { addAccount(account) })
      }
    })
  }

  private fun loadRepositories(account: GiteeAccount,
                               executor: GiteeApiRequestExecutor.WithTokensAuth) {
    repositoriesByAccount.remove(account)
    errorsByAccount.remove(account)

    progressManager.run(object : Task.Backgroundable(project, "Not Visible") {
      override fun run(indicator: ProgressIndicator) {
        val repoPagesRequest = GiteeApiRequests.CurrentUser.Repos.pages(account.server,
                                                                         affiliation = Affiliation.combine(Affiliation.OWNER,
                                                                                                           Affiliation.COLLABORATOR),
                                                                         pagination = GiteeRequestPagination.DEFAULT)
        val pageItemsConsumer: (List<GiteeRepo>) -> Unit = {
          runInEdt {
            repositoriesByAccount.getOrPut(account, { UpdateOrderLinkedHashSet() }).addAll(it)
            refillRepositories()
          }
        }
        GiteeApiPagesLoader.loadAll(executor, indicator, repoPagesRequest, pageItemsConsumer)

        val orgsRequest = GiteeApiRequests.CurrentUser.Orgs.pages(account.server)
        val userOrganizations = GiteeApiPagesLoader.loadAll(executor, indicator, orgsRequest).sortedBy { it.login }

        for (org in userOrganizations) {
          val orgRepoRequest = GiteeApiRequests.Organisations.Repos.pages(account.server, org.login, GiteeRequestPagination.DEFAULT)
          GiteeApiPagesLoader.loadAll(executor, indicator, orgRepoRequest, pageItemsConsumer)
        }
      }

      override fun onThrowable(error: Throwable) {
        LOG.error(error)
        errorsByAccount[account] = GiteeRepositoryListItem.Error(account,
                                                              GiteeBundle.message("clone.error.load.repositories"),
                                                              GiteeBundle.message("retry.link"),
                                                              Runnable { loadRepositories(account, executor) })
      }
    })
  }

  private fun refillRepositories() {
    val selectedValue = repositoryList.selectedValue
    originListModel.removeAll()
    for (account in getAccounts()) {
      if (errorsByAccount[account] != null) {
        originListModel.add(errorsByAccount[account])
      }
      val user = userDetailsByAccount[account] ?: continue
      val repos = repositoriesByAccount[account] ?: continue
      for (repo in repos) {
        originListModel.add(GiteeRepositoryListItem.Repo(account, user, repo))
      }
    }
    repositoryList.setSelectedValue(selectedValue, false)
    ScrollingUtil.ensureSelectionExists(repositoryList)
  }

  override fun getView() = wrapper

  override fun doValidateAll(): List<ValidationInfo> {
    val list = ArrayList<ValidationInfo>()
    ContainerUtil.addIfNotNull(list, CloneDvcsValidationUtils.checkDirectory(directoryField.text, directoryField.textField))
    return list
  }

  override fun doClone(checkoutListener: CheckoutProvider.Listener) {
    val parent = Paths.get(directoryField.text).toAbsolutePath().parent
    val destinationValidation = CloneDvcsValidationUtils.createDestination(parent.toString())
    if (destinationValidation != null) {
      LOG.error("Unable to create destination directory", destinationValidation.message)
      GiteeNotifications.showError(project, GiteeBundle.message("clone.dialog.clone.failed"),
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
      GiteeNotifications.showError(project, GiteeBundle.message("clone.dialog.clone.failed"),
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
    val giteeRepoPath = getGiteeRepoPath(searchField.text)
    if (giteeRepoPath != null) {
      selectedUrl = giteeGitHelper.getRemoteUrl(
        giteeRepoPath.serverPath,
        giteeRepoPath.repositoryPath.owner,
        giteeRepoPath.repositoryPath.repository
      )
      repositoryList.emptyText.appendText("Clone '$selectedUrl'")
      return
    }
    val selectedValue = repositoryList.selectedValue
    if (selectedValue is GiteeRepositoryListItem.Repo) {
      selectedUrl = giteeGitHelper.getRemoteUrl(
        selectedValue.account.server,
        selectedValue.repo.userName,
        selectedValue.repo.name
      )
      return
    }
    selectedUrl = null
  }


  private fun getGiteeRepoPath(searchText: String): GiteeRepositoryCoordinates? {
    val url = searchText
      .trim()
      .removePrefix("git clone")
      .removeSuffix(".git")
      .trim()

    try {
      var serverPath = GiteeServerPath.from(url)
      serverPath = GiteeServerPath.from(serverPath.toUrl().removeSuffix(serverPath.suffix ?: ""))

      val giteeFullPath = GiteeUrlUtil.getUserAndRepositoryFromRemoteUrl(url) ?: return null
      return GiteeRepositoryCoordinates(serverPath, giteeFullPath)
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
      directoryField.trySetChildPath(path)
    }
  }

  /**
   * Since each repository can be in several states at the same time (shared access for a collaborator and shared access for org member) and
   * repositories for collaborators are loaded in separate request before repositories for org members, we need to update order of re-added
   * repo in order to place it close to other organization repos
   */
  private class UpdateOrderLinkedHashSet<T> : LinkedHashSet<T>() {
    override fun add(element: T): Boolean {
      val wasThere = remove(element)
      super.add(element)
      // Contract is "true if this set did not already contain the specified element"
      return !wasThere
    }
  }

  private fun resizeIcon(icon: Icon, size: Int): Icon {
    val scale = JBUI.scale(size).toFloat() / icon.iconWidth.toFloat()
    return IconUtil.scale(icon, null, scale)
  }

  protected abstract fun createAccountMenuLoginActions(account: GiteeAccount?): Collection<Action>

  private fun showPopupMenu() {
    val menuItems = mutableListOf<AccountMenuItem>()
    val project = ProjectManager.getInstance().defaultProject

    for ((index, account) in getAccounts().withIndex()) {
      val user = userDetailsByAccount[account]

      val accountTitle = user?.login ?: account.name
      val serverInfo = account.server.toUrl().removePrefix("http://").removePrefix("https://")
      val avatar = avatarsByAccount[account] ?: defaultPopupAvatar
      val accountActions = mutableListOf<Action>()
      val showSeparatorAbove = index != 0

      if (user == null) {
        accountActions += createAccountMenuLoginActions(account)
        accountActions += Action(GiteeBundle.message("accounts.remove"), { authenticationManager.removeAccount(account) },
                                 showSeparatorAbove = true)
      }
      else {
        if (account != authenticationManager.getDefaultAccount(project)) {
          accountActions += Action(GiteeBundle.message("accounts.set.default"),
                                   { authenticationManager.setDefaultAccount(project, account) })
        }
        accountActions += Action(GiteeBundle.message("open.on.gitee.action"), { BrowserUtil.browse(user.htmlUrl) },
                                 AllIcons.Ide.External_link_arrow)
        accountActions += Action(GiteeBundle.message("accounts.log.out"), { authenticationManager.removeAccount(account) },
                                 showSeparatorAbove = true)
      }

      menuItems += Account(accountTitle, serverInfo, avatar, accountActions, showSeparatorAbove)
    }
    menuItems += createAccountMenuLoginActions(null)

    AccountsMenuListPopup(null, AccountMenuPopupStep(menuItems)).showUnderneathOf(accountsPanel)
  }

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
}
