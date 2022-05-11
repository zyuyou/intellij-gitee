// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.toolwindow

import com.gitee.api.data.GiteePullRequest
import com.gitee.i18n.GiteeBundle
import com.gitee.pullrequest.action.GEPRActionKeys
import com.gitee.pullrequest.config.GiteePullRequestsProjectUISettings
import com.gitee.pullrequest.data.GEListLoader
import com.gitee.pullrequest.data.GEPRDataContext
import com.gitee.pullrequest.data.GEPRListUpdatesChecker
import com.gitee.pullrequest.data.GEPRSearchQuery
import com.gitee.pullrequest.search.GEPRSearchCompletionProvider
import com.gitee.pullrequest.search.GEPRSearchQueryHolder
import com.gitee.pullrequest.ui.GEApiLoadingErrorHandler
import com.gitee.ui.component.GEHandledErrorPanelModel
import com.gitee.ui.component.GEHtmlErrorPanel
import com.gitee.ui.util.BoundedRangeModelThresholdListener
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.collaboration.ui.codereview.OpenReviewButton
import com.intellij.collaboration.ui.codereview.OpenReviewButtonViewModel
import com.intellij.ide.DataManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.progress.util.ProgressWindow
import com.intellij.openapi.project.Project
import com.intellij.ui.*
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.ListUiUtil
import com.intellij.util.ui.StatusText
import com.intellij.util.ui.UIUtil
import com.intellij.vcs.log.ui.frame.ProgressStripe
import java.awt.FlowLayout
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.ListSelectionModel

internal object GEPRListComponent {

  fun create(project: Project,
             dataContext: GEPRDataContext,
             disposable: Disposable): JComponent {

    val actionManager = ActionManager.getInstance()

    val listLoader = dataContext.listLoader
    val listModel = CollectionListModel(listLoader.loadedData)
    listLoader.addDataListener(disposable, object : GEListLoader.ListDataListener {
      override fun onDataAdded(startIdx: Int) {
        val loadedData = listLoader.loadedData
        listModel.add(loadedData.subList(startIdx, loadedData.size))
      }

      override fun onDataUpdated(idx: Int) = listModel.setElementAt(listLoader.loadedData[idx], idx)
      override fun onDataRemoved(data: Any) {
        (data as? GiteePullRequest)?.let { listModel.remove(it) }
      }

      override fun onAllDataRemoved() = listModel.removeAll()
    })

    val list = object : JBList<GiteePullRequest>(listModel) {

      override fun getToolTipText(event: MouseEvent): String? {
        val childComponent = ListUtil.getDeepestRendererChildComponentAt(this, event.point)
        if (childComponent !is JComponent) return null
        return childComponent.toolTipText
      }
    }.apply {
      setExpandableItemsEnabled(false)
      emptyText.clear()
      selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
    }.also {
      ScrollingUtil.installActions(it)
      ListUtil.installAutoSelectOnMouseMove(it)
      ListUiUtil.Selection.installSelectionOnFocus(it)
      ListUiUtil.Selection.installSelectionOnRightClick(it)
      DataManager.registerDataProvider(it) { dataId ->
        if (GEPRActionKeys.SELECTED_PULL_REQUEST.`is`(dataId)) it.selectedValue else null
      }
      val groupId = "Gitee.PullRequest.ToolWindow.List.Popup"
      PopupHandler.installSelectionListPopup(it, actionManager.getAction(groupId) as ActionGroup, groupId)
      val shortcuts = CompositeShortcutSet(CommonShortcuts.ENTER, CommonShortcuts.DOUBLE_CLICK_1)
      EmptyAction.registerWithShortcutSet("Gitee.PullRequest.Show", shortcuts, it)
      ListSpeedSearch(it) { item -> item.title }
    }

    val openButtonViewModel = OpenReviewButtonViewModel()
    OpenReviewButton.installOpenButtonListeners(list, openButtonViewModel) {
      ActionManager.getInstance().getAction("Gitee.PullRequest.Show")
    }

    val renderer = GEPRListCellRenderer(dataContext.avatarIconsProvider, openButtonViewModel)
    list.cellRenderer = renderer
    UIUtil.putClientProperty(list, UIUtil.NOT_IN_HIERARCHY_COMPONENTS, listOf(renderer))

    val searchQueryHolder = dataContext.searchHolder
    val searchStringModel = SingleValueModel(searchQueryHolder.queryString)
    searchQueryHolder.addQueryChangeListener(disposable) {
      if (searchStringModel.value != searchQueryHolder.queryString)
        searchStringModel.value = searchQueryHolder.queryString
    }
    searchStringModel.addListener {
      searchQueryHolder.queryString = searchStringModel.value
    }

    ListEmptyTextController(listLoader, searchQueryHolder, list.emptyText, disposable)

    val searchCompletionProvider = GEPRSearchCompletionProvider(project, dataContext.repositoryDataService)
    val pullRequestUiSettings = GiteePullRequestsProjectUISettings.getInstance(project)
    val search = GEPRSearchPanel.create(project, searchStringModel, searchCompletionProvider, pullRequestUiSettings).apply {
      border = IdeBorderFactory.createBorder(SideBorder.BOTTOM)
    }

    val outdatedStatePanel = JPanel(FlowLayout(FlowLayout.LEFT, JBUIScale.scale(5), 0)).apply {
      background = UIUtil.getPanelBackground()
      border = JBUI.Borders.empty(4, 0)
      add(JLabel(GiteeBundle.message("pull.request.list.outdated")))
      add(ActionLink(GiteeBundle.message("pull.request.list.refresh")) {
        listLoader.reset()
      })

      isVisible = false
    }
    OutdatedPanelController(listLoader, dataContext.listUpdatesChecker, outdatedStatePanel, disposable)

    val errorHandler = GEApiLoadingErrorHandler(project, dataContext.securityService.account) {
      listLoader.reset()
    }
    val errorModel = GEHandledErrorPanelModel(GiteeBundle.message("pull.request.list.cannot.load"), errorHandler).apply {
      error = listLoader.error
    }
    listLoader.addErrorChangeListener(disposable) {
      errorModel.error = listLoader.error
    }
    val errorPane = GEHtmlErrorPanel.create(errorModel)

    val controlsPanel = JPanel(VerticalLayout(0)).apply {
      isOpaque = false
      add(search)
      add(outdatedStatePanel)
      add(errorPane)
    }
    val listLoaderPanel = createListLoaderPanel(listLoader, list, disposable)
    return JBUI.Panels.simplePanel(listLoaderPanel).addToTop(controlsPanel).andTransparent().also {
      DataManager.registerDataProvider(it) { dataId ->
        if (GEPRActionKeys.SELECTED_PULL_REQUEST.`is`(dataId)) {
          if (list.isSelectionEmpty) null else list.selectedValue
        }
        else null
      }
      actionManager.getAction("Gitee.PullRequest.List.Reload").registerCustomShortcutSet(it, disposable)
    }
  }

  private fun createListLoaderPanel(loader: GEListLoader<*>, list: JBList<GiteePullRequest>, disposable: Disposable): JComponent {

    val scrollPane = ScrollPaneFactory.createScrollPane(list, true).apply {
      isOpaque = false
      viewport.isOpaque = false

      verticalScrollBar.apply {
        isOpaque = true
        UIUtil.putClientProperty(this, JBScrollPane.IGNORE_SCROLLBAR_IN_INSETS, false)
      }

      BoundedRangeModelThresholdListener.install(verticalScrollBar) {
        if (loader.canLoadMore()) loader.loadMore()
      }
    }
    loader.addDataListener(disposable, object : GEListLoader.ListDataListener {
      override fun onAllDataRemoved() {
        if (scrollPane.isShowing) loader.loadMore()
      }
    })
    val progressStripe = ProgressStripe(scrollPane, disposable,
                                        ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS).apply {
      if (loader.loading) startLoadingImmediately() else stopLoading()
    }
    loader.addLoadingStateChangeListener(disposable) {
      if (loader.loading) progressStripe.startLoading() else progressStripe.stopLoading()
    }
    return progressStripe
  }


  private class ListEmptyTextController(private val listLoader: GEListLoader<*>,
                                        private val searchHolder: GEPRSearchQueryHolder,
                                        private val emptyText: StatusText,
                                        listenersDisposable: Disposable) {
    init {
      listLoader.addLoadingStateChangeListener(listenersDisposable, ::update)
      searchHolder.addQueryChangeListener(listenersDisposable, ::update)
    }

    private fun update() {
      emptyText.clear()
      if (listLoader.loading || listLoader.error != null) return


      val query = searchHolder.query
      if (query == GEPRSearchQuery.DEFAULT) {
        emptyText.appendText(GiteeBundle.message("pull.request.list.no.matches"))
          .appendSecondaryText(GiteeBundle.message("pull.request.list.reset.filters"),
                               SimpleTextAttributes.LINK_ATTRIBUTES,
                               ActionListener { searchHolder.query = GEPRSearchQuery.EMPTY })
      }
      else if (query.isEmpty()) {
        emptyText.appendText(GiteeBundle.message("pull.request.list.nothing.loaded"))
      }
      else {
        emptyText.appendText(GiteeBundle.message("pull.request.list.no.matches"))
          .appendSecondaryText(GiteeBundle.message("pull.request.list.reset.filters.to.default", GEPRSearchQuery.DEFAULT.toString()),
                               SimpleTextAttributes.LINK_ATTRIBUTES,
                               ActionListener { searchHolder.query = GEPRSearchQuery.DEFAULT })
      }
    }
  }

  private class OutdatedPanelController(private val listLoader: GEListLoader<*>,
                                        private val listChecker: GEPRListUpdatesChecker,
                                        private val panel: JPanel,
                                        listenersDisposable: Disposable) {
    init {
      listLoader.addLoadingStateChangeListener(listenersDisposable, ::update)
      listLoader.addErrorChangeListener(listenersDisposable, ::update)
      listChecker.addOutdatedStateChangeListener(listenersDisposable, ::update)
    }

    private fun update() {
      panel.isVisible = listChecker.outdated && (!listLoader.loading && listLoader.error == null)
    }
  }
}