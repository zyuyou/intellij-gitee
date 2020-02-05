// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.data.GiteePullRequestDetailed
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.pullrequest.action.GiteePRActionDataContext
import com.gitee.pullrequest.action.GiteePullRequestKeys
import com.gitee.pullrequest.avatars.CachingGiteeAvatarIconsProvider
import com.gitee.pullrequest.comment.GiteePRDiffReviewThreadsProviderImpl
import com.gitee.pullrequest.comment.ui.GiteePREditorReviewThreadComponentFactoryImpl
import com.gitee.pullrequest.config.GiteePullRequestsProjectUISettings
import com.gitee.pullrequest.data.GiteePRDataContext
import com.gitee.pullrequest.data.GiteePRDataContextRepository
import com.gitee.pullrequest.data.GiteePRDataProvider
import com.gitee.pullrequest.search.GiteePullRequestSearchPanel
import com.gitee.pullrequest.ui.*
import com.gitee.pullrequest.ui.changes.GiteePRChangesBrowser
import com.gitee.pullrequest.ui.changes.GiteePRChangesModel
import com.gitee.pullrequest.ui.changes.GiteePRChangesModelImpl
import com.gitee.pullrequest.ui.details.GiteePullRequestDetailsPanel
import com.gitee.ui.util.SingleValueModel
import com.gitee.util.*
import com.intellij.codeInsight.AutoPopupController
import com.intellij.diff.editor.VCSContentVirtualFile
import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.Service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.EditorWindowHolder
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Splitter
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.*
import com.intellij.ui.components.JBPanelWithEmptyText
import com.intellij.util.ui.UIUtil
import git4idea.GitCommit
import org.jetbrains.annotations.CalledInAwt
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.ActionListener
import javax.swing.JComponent
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener
import javax.swing.event.ListSelectionEvent

@Service
internal class GiteePRComponentFactory(private val project: Project) {

  private val progressManager = ProgressManager.getInstance()
  private val actionManager = ActionManager.getInstance()
  private val copyPasteManager = CopyPasteManager.getInstance()
  private val avatarLoader = CachingGiteeUserAvatarLoader.getInstance()
  private val imageResizer = GiteeImageResizer.getInstance()

  private var ghprVirtualFile: VCSContentVirtualFile? = null
  private var ghprEditorContent: JComponent? = null

  private val autoPopupController = AutoPopupController.getInstance(project)
  private val projectUiSettings = GiteePullRequestsProjectUISettings.getInstance(project)
  private val dataContextRepository = GiteePRDataContextRepository.getInstance(project)

  @CalledInAwt
  fun createComponent(remoteUrl: GitRemoteUrlCoordinates, account: GiteeAccount, requestExecutor: GiteeApiRequestExecutor,
                      parentDisposable: Disposable): JComponent {

    val contextValue = object : LazyCancellableBackgroundProcessValue<GiteePRDataContext>(progressManager) {
      override fun compute(indicator: ProgressIndicator) =
        dataContextRepository.getContext(indicator, account, requestExecutor, remoteUrl).also {
          Disposer.register(parentDisposable, it)
        }
    }
    Disposer.register(parentDisposable, Disposable { contextValue.drop() })

    val uiDisposable = Disposer.newDisposable()
    Disposer.register(parentDisposable, uiDisposable)

    val loadingModel = GiteeCompletableFutureLoadingModel<GiteePRDataContext>()
    val contentContainer = JBPanelWithEmptyText(null).apply {
      background = UIUtil.getListBackground()
    }
    loadingModel.addStateChangeListener(object : GiteeLoadingModel.StateChangeListener {
      override fun onLoadingCompleted() {
        val dataContext = loadingModel.result
        if (dataContext != null) {
          var content = createContent(dataContext, uiDisposable)
          if (Registry.`is`("show.log.as.editor.tab")) {
            content = patchContent(content)
          }

          with(contentContainer) {
            layout = BorderLayout()
            add(content, BorderLayout.CENTER)
            validate()
            repaint()
          }
        }
      }
    })
    loadingModel.future = contextValue.value

    return GiteeLoadingPanel(loadingModel, contentContainer, uiDisposable,
      GiteeLoadingPanel.EmptyTextBundle.Simple("", "Can't load data from Gitee")).apply {
      resetHandler = ActionListener {
        contextValue.drop()
        loadingModel.future = contextValue.value
      }
    }
  }

  private fun patchContent(content: JComponent): JComponent {
    var patchedContent = content
    val onePixelSplitter = patchedContent as OnePixelSplitter
    val splitter = onePixelSplitter.secondComponent as Splitter
    patchedContent = splitter.secondComponent

    onePixelSplitter.secondComponent = splitter.firstComponent
    installEditor(onePixelSplitter)
    return patchedContent
  }

  private fun installEditor(onePixelSplitter: OnePixelSplitter) {
    ghprEditorContent = onePixelSplitter
    ApplicationManager.getApplication().invokeLater({ tryOpenGiteePREditorTab() }, ModalityState.NON_MODAL)
  }

  @CalledInAwt
  private fun getOrCreateGiteePRViewFile(): VirtualFile? {
    ApplicationManager.getApplication().assertIsDispatchThread()

    if (ghprEditorContent == null) return null

    if (ghprVirtualFile == null) {
      val content = ghprEditorContent ?: error("editor content should be created by this time")
      ghprVirtualFile = VCSContentVirtualFile(content) { "Gitee Pull Requests" }
      ghprVirtualFile?.putUserData(VCSContentVirtualFile.TabSelector) {
        GiteeUIUtil.findAndSelectGiteeContent(project, true)
      }
    }

    Disposer.register(project, Disposable { ghprVirtualFile = null })

    return ghprVirtualFile ?: error("error")
  }

  fun tryOpenGiteePREditorTab() {
    val file = getOrCreateGiteePRViewFile() ?: return

    val editors = FileEditorManager.getInstance(project).openFile(file, true)
    assert(editors.size == 1) { "opened multiple log editors for $file" }
    val editor = editors[0]
    val component = editor.component
    val holder = ComponentUtil.getParentOfType(EditorWindowHolder::class.java as Class<out EditorWindowHolder>, component as Component)
      ?: return
    val editorWindow = holder.editorWindow
    editorWindow.setFilePinned(file, true)
  }

  private fun createContent(dataContext: GiteePRDataContext, disposable: Disposable): JComponent {
    val avatarIconsProviderFactory = CachingGiteeAvatarIconsProvider.Factory(avatarLoader, imageResizer, dataContext.requestExecutor)
    val listSelectionHolder = GiteePullRequestsListSelectionHolderImpl()
    val actionDataContext = GiteePRActionDataContext(dataContext, listSelectionHolder, avatarIconsProviderFactory)

    val list = GiteePullRequestsList(copyPasteManager, avatarIconsProviderFactory, dataContext.listModel).apply {
      emptyText.clear()
    }.also {
      installPopup(it)
      installSelectionSaver(it, listSelectionHolder)
    }

    val search = GiteePullRequestSearchPanel(project, autoPopupController, dataContext.searchHolder).apply {
      border = IdeBorderFactory.createBorder(SideBorder.BOTTOM)
    }
    val loaderPanel = GiteePRListLoaderPanel(dataContext.listLoader, dataContext.dataLoader, list, search)

    val dataProviderModel = createDataProviderModel(dataContext, listSelectionHolder, disposable)

    val detailsLoadingModel = createDetailsLoadingModel(dataProviderModel, disposable)
    val detailsModel = createValueModel(detailsLoadingModel)

    val detailsPanel = GiteePullRequestDetailsPanel(project, detailsModel,
      dataContext.securityService,
      dataContext.busyStateTracker,
      dataContext.metadataService,
      dataContext.stateService,
      avatarIconsProviderFactory)
    Disposer.register(disposable, detailsPanel)

    val detailsLoadingPanel = GiteeLoadingPanel(detailsLoadingModel, detailsPanel, disposable,
      GiteeLoadingPanel.EmptyTextBundle.Simple("Select pull request to view details",
        "Can't load details")).apply {
      resetHandler = ActionListener { dataProviderModel.value?.reloadDetails() }
    }

    val changesLoadingModel = createChangesLoadingModel(dataProviderModel, disposable)
    val changesModel = createChangesModel(projectUiSettings, changesLoadingModel, disposable)
    val changesBrowser = GiteePRChangesBrowser(changesModel, project)

    val diffCommentComponentFactory = GiteePREditorReviewThreadComponentFactoryImpl(avatarIconsProviderFactory)
    dataProviderModel.addValueChangedListener {
      changesBrowser.diffReviewThreadsProvider = dataProviderModel.value?.let {
        GiteePRDiffReviewThreadsProviderImpl(it, diffCommentComponentFactory)
      }
    }

    val changesLoadingPanel = GiteeLoadingPanel(changesLoadingModel, changesBrowser, disposable,
      GiteeLoadingPanel.EmptyTextBundle.Simple("Select pull request to view changes",
        "Can't load changes",
        "Pull request does not contain any changes")).apply {
      resetHandler = ActionListener { dataProviderModel.value?.reloadCommits() }
    }

    Disposer.register(disposable, Disposable {
      Disposer.dispose(list)
      Disposer.dispose(search)
      Disposer.dispose(loaderPanel)

      Disposer.dispose(detailsPanel)
    })

    return OnePixelSplitter("Gitee.PullRequests.Component", 0.33f).apply {
      background = UIUtil.getListBackground()
      isOpaque = true
      isFocusCycleRoot = true
      firstComponent = loaderPanel
      secondComponent = OnePixelSplitter("Gitee.PullRequest.Preview.Component", 0.5f).apply {
        firstComponent = detailsLoadingPanel
        secondComponent = changesLoadingPanel
      }
    }.also {
      changesBrowser.diffAction.registerCustomShortcutSet(it, disposable)
      DataManager.registerDataProvider(it) { dataId ->
        if (Disposer.isDisposed(disposable)) null
        else when {
          GiteePullRequestKeys.ACTION_DATA_CONTEXT.`is`(dataId) -> actionDataContext
          else -> null
        }

      }
    }
  }

  private fun openTimelineForSelection(dataContext: GiteePRDataContext,
                                       actionDataContext: GiteePRActionDataContext,
                                       list: GiteePullRequestsList) {
    val pullRequest = list.selectedValue
    val file = GiteePRVirtualFile(actionDataContext,
      pullRequest,
      dataContext.dataLoader.getDataProvider(pullRequest.number))
    FileEditorManager.getInstance(project).openFile(file, true)
  }

  private fun installPopup(list: GiteePullRequestsList) {
    val popupHandler = object : PopupHandler() {
      override fun invokePopup(comp: java.awt.Component, x: Int, y: Int) {
        if (ListUtil.isPointOnSelection(list, x, y)) {
          val popupMenu = actionManager
            .createActionPopupMenu("GiteePullRequestListPopup",
              actionManager.getAction("Gitee.PullRequest.ToolWindow.List.Popup") as ActionGroup)
          popupMenu.setTargetComponent(list)
          popupMenu.component.show(comp, x, y)
        }
      }
    }
    list.addMouseListener(popupHandler)
  }

  private fun installSelectionSaver(list: GiteePullRequestsList, listSelectionHolder: GiteePullRequestsListSelectionHolder) {
    var savedSelectionNumber: Long? = null

    list.selectionModel.addListSelectionListener { e: ListSelectionEvent ->
      if (!e.valueIsAdjusting) {
        val selectedIndex = list.selectedIndex
        if (selectedIndex >= 0 && selectedIndex < list.model.size) {
          listSelectionHolder.selectionNumber = list.model.getElementAt(selectedIndex).number
          savedSelectionNumber = null
        }
      }
    }

    list.model.addListDataListener(object : ListDataListener {
      override fun intervalAdded(e: ListDataEvent) {
        if (e.type == ListDataEvent.INTERVAL_ADDED)
          (e.index0..e.index1).find { list.model.getElementAt(it).number == savedSelectionNumber }
            ?.run { ApplicationManager.getApplication().invokeLater { ScrollingUtil.selectItem(list, this) } }
      }

      override fun contentsChanged(e: ListDataEvent) {}
      override fun intervalRemoved(e: ListDataEvent) {
        if (e.type == ListDataEvent.INTERVAL_REMOVED) savedSelectionNumber = listSelectionHolder.selectionNumber
      }
    })
  }

  private fun createChangesModel(projectUiSettings: GiteePullRequestsProjectUISettings,
                                 loadingModel: GiteeLoadingModel<List<GitCommit>>,
                                 parentDisposable: Disposable): GiteePRChangesModel {
    val model = GiteePRChangesModelImpl(projectUiSettings.zipChanges)
    loadingModel.addStateChangeListener(object : GiteeLoadingModel.StateChangeListener {
      override fun onLoadingCompleted() {
        model.commits = loadingModel.result.orEmpty()
      }

      override fun onReset() {
        model.commits = loadingModel.result.orEmpty()
      }
    })
    projectUiSettings.addChangesListener(parentDisposable) {
      model.zipChanges = projectUiSettings.zipChanges
    }
    return model
  }

  private fun createChangesLoadingModel(dataProviderModel: SingleValueModel<GiteePRDataProvider?>,
                                        parentDisposable: Disposable): GiteeCompletableFutureLoadingModel<List<GitCommit>> {
    val model = GiteeCompletableFutureLoadingModel<List<GitCommit>>()

    var listenerDisposable: Disposable? = null

    dataProviderModel.addValueChangedListener {
      val provider = dataProviderModel.value
      model.future = provider?.logCommitsRequest

      listenerDisposable = listenerDisposable?.let {
        Disposer.dispose(it)
        null
      }

      if (provider != null) {
        val disposable = Disposer.newDisposable().apply {
          Disposer.register(parentDisposable, this)
        }
        provider.addRequestsChangesListener(disposable, object : GiteePRDataProvider.RequestsChangedListener {
          override fun commitsRequestChanged() {
            model.future = provider.logCommitsRequest
          }
        })

        listenerDisposable = disposable
      }
    }

    return model
  }

  private fun createDetailsLoadingModel(dataProviderModel: SingleValueModel<GiteePRDataProvider?>,
                                        parentDisposable: Disposable): GiteeCompletableFutureLoadingModel<GiteePullRequestDetailed> {
    val model = GiteeCompletableFutureLoadingModel<GiteePullRequestDetailed>()

    var listenerDisposable: Disposable? = null

    dataProviderModel.addValueChangedListener {
      val provider = dataProviderModel.value
      model.future = provider?.detailsRequest

      listenerDisposable = listenerDisposable?.let {
        Disposer.dispose(it)
        null
      }

      if (provider != null) {
        val disposable = Disposer.newDisposable().apply {
          Disposer.register(parentDisposable, this)
        }
        provider.addRequestsChangesListener(disposable, object : GiteePRDataProvider.RequestsChangedListener {
          override fun detailsRequestChanged() {
            model.future = provider.detailsRequest
          }
        })

        listenerDisposable = disposable
      }
    }

    return model
  }

  private fun <T> createValueModel(loadingModel: GiteeLoadingModel<T>): SingleValueModel<T?> {
    val model = SingleValueModel<T?>(null)
    loadingModel.addStateChangeListener(object : GiteeLoadingModel.StateChangeListener {
      override fun onLoadingCompleted() {
        model.value = loadingModel.result
      }

      override fun onReset() {
        model.value = loadingModel.result
      }
    })
    return model
  }

  private fun createDataProviderModel(dataContext: GiteePRDataContext,
                                      listSelectionHolder: GiteePullRequestsListSelectionHolder,
                                      parentDisposable: Disposable): SingleValueModel<GiteePRDataProvider?> {
    val model: SingleValueModel<GiteePRDataProvider?> = SingleValueModel(null)

    fun setNewProvider(provider: GiteePRDataProvider?) {
      val oldValue = model.value
      if (oldValue != null && provider != null && oldValue.number != provider.number) {
        model.value = null
      }
      model.value = provider
    }

    listSelectionHolder.addSelectionChangeListener(parentDisposable) {
      setNewProvider(listSelectionHolder.selectionNumber?.let(dataContext.dataLoader::getDataProvider))
    }

    dataContext.dataLoader.addInvalidationListener(parentDisposable) {
      val selection = listSelectionHolder.selectionNumber
      if (selection != null && selection == it) {
        setNewProvider(dataContext.dataLoader.getDataProvider(selection))
      }
    }

    return model
  }
}