/*
 *  Copyright 2016-2019 码云 - Gitee
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.gitee.pullrequest.ui

import com.gitee.api.data.GiteePullRequest
import com.gitee.exceptions.GiteeStatusCodeException
import com.gitee.pullrequest.action.GiteePullRequestKeys
import com.gitee.pullrequest.avatars.CachingGiteeAvatarIconsProvider
import com.gitee.pullrequest.data.GiteePullRequestsLoader
import com.gitee.pullrequest.search.GiteePullRequestSearchComponent
import com.gitee.pullrequest.search.GiteePullRequestSearchModel
import com.gitee.util.GiteeAsyncUtil
import com.gitee.util.ProgressStripeProgressIndicator
import com.gitee.util.handleOnEdt
import com.intellij.codeInsight.AutoPopupController
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.progress.util.ProgressWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.*
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import com.intellij.vcs.log.ui.frame.ProgressStripe
import org.jetbrains.annotations.CalledInAwt
import java.awt.Component
import javax.swing.JScrollBar
import javax.swing.ScrollPaneConstants
import javax.swing.event.ListSelectionEvent

/**
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/pullrequest/ui/GithubPullRequestsListComponent.kt
 * @author JetBrains s.r.o.
 */
internal class GiteePullRequestsListComponent(project: Project,
                                              copyPasteManager: CopyPasteManager,
                                              actionManager: ActionManager,
                                              autoPopupController: AutoPopupController,
                                              private val loader: GiteePullRequestsLoader,
                                              avatarIconsProviderFactory: CachingGiteeAvatarIconsProvider.Factory)
  : BorderLayoutPanel(), Disposable, DataProvider {

  val selectionModel = GiteePullRequestsListSelectionModel()
  private val listModel = CollectionListModel<GiteePullRequest>()
  private val list = GiteePullRequestsList(copyPasteManager, avatarIconsProviderFactory, listModel)
  private val scrollPane = ScrollPaneFactory.createScrollPane(list,
                                                              ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                              ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER).apply {
    border = JBUI.Borders.empty()
    verticalScrollBar.model.addChangeListener { potentiallyLoadMore() }
  }
  private var loadOnScrollThreshold = true
  private var isDisposed = false
  private val errorPanel = HtmlErrorPanel()
  private val progressStripe = ProgressStripe(scrollPane, this, ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS)
  private var progressIndicator = ProgressStripeProgressIndicator(progressStripe, true)

  private val searchModel = GiteePullRequestSearchModel()
  private val search = GiteePullRequestSearchComponent(project, autoPopupController, searchModel).apply {
    border = IdeBorderFactory.createBorder(SideBorder.BOTTOM)
  }

  init {
    searchModel.addListener(object : GiteePullRequestSearchModel.StateListener {
      override fun queryChanged() {
        loader.setSearchQuery(searchModel.query)
        refresh()
      }
    }, this)

    list.selectionModel.addListSelectionListener { e: ListSelectionEvent ->
      if (!e.valueIsAdjusting) {
        if (list.selectedIndex < 0) selectionModel.current = null
        else selectionModel.current = listModel.getElementAt(list.selectedIndex)
      }
    }

    val popupHandler = object : PopupHandler() {
      override fun invokePopup(comp: Component, x: Int, y: Int) {
        if (ListUtil.isPointOnSelection(list, x, y)) {
          val popupMenu = actionManager
            .createActionPopupMenu("GiteePullRequestListPopup",
                                   actionManager.getAction("Gitee.PullRequest.ToolWindow.List.Popup") as ActionGroup)
          popupMenu.setTargetComponent(this@GiteePullRequestsListComponent)
          popupMenu.component.show(comp, x, y)
        }
      }
    }
    list.addMouseListener(popupHandler)

    val tableWithError = JBUI.Panels
      .simplePanel(progressStripe)
      .addToTop(errorPanel)

    addToTop(search)
    addToCenter(tableWithError)

    resetSearch()

    Disposer.register(this, list)
  }

  override fun getData(dataId: String): Any? {
    return if (GiteePullRequestKeys.SELECTED_PULL_REQUEST.`is`(dataId)) selectionModel.current else null
  }

  @CalledInAwt
  fun refresh() {
    loadOnScrollThreshold = false
    list.selectionModel.clearSelection()
    listModel.removeAll()
    progressIndicator.cancel()
    progressIndicator = ProgressStripeProgressIndicator(progressStripe, true)
    loader.reset()
    loadMore()
  }

  private fun potentiallyLoadMore() {
    if (loadOnScrollThreshold && isScrollAtThreshold(scrollPane.verticalScrollBar)) {
      loadMore()
    }
  }

  private fun loadMore() {
    if (isDisposed) return
    loadOnScrollThreshold = false
    errorPanel.setError(null)

    list.emptyText.text = "Loading pull requests..."
    val indicator = progressIndicator

    loader.requestLoadMore(indicator).handleOnEdt { responsePage, error ->
      if (indicator.isCanceled) return@handleOnEdt

      when {
        error != null && !GiteeAsyncUtil.isCancellation(error) -> {
          loadingErrorOccurred(error)
        }
        responsePage != null -> {
          moreDataLoaded(responsePage.list, responsePage.nextLink != null)
        }
      }
    }
  }

  private fun isScrollAtThreshold(verticalScrollBar: JScrollBar): Boolean {
    val visibleAmount = verticalScrollBar.visibleAmount
    val value = verticalScrollBar.value
    val maximum = verticalScrollBar.maximum
    if (maximum == 0) return false
    val scrollFraction = (visibleAmount + value) / maximum.toFloat()
    if (scrollFraction < 0.5) return false
    return true
  }

  private fun moreDataLoaded(data: List<GiteePullRequest>, hasNext: Boolean) {
    if (searchModel.query.isEmpty()) {
      list.emptyText.text = "No pull requests loaded."
    }
    else {
      list.emptyText.text = "No pull requests matching filters."
      list.emptyText.appendSecondaryText("Reset Filters", SimpleTextAttributes.LINK_ATTRIBUTES) {
        resetSearch()
      }
    }
    loadOnScrollThreshold = hasNext
    listModel.add(data)

    //otherwise scrollbar will have old values (before data insert)
    scrollPane.viewport.validate()
    potentiallyLoadMore()
  }

  private fun resetSearch() {
    search.searchText = "state:open"
  }

  private fun loadingErrorOccurred(error: Throwable) {
    loadOnScrollThreshold = false

    val prefix = if (list.isEmpty) "Cannot load pull requests." else "Cannot load full pull requests list."

    list.emptyText.clear().appendText(prefix, SimpleTextAttributes.ERROR_ATTRIBUTES)
      .appendSecondaryText(getLoadingErrorText(error), SimpleTextAttributes.ERROR_ATTRIBUTES, null)
      .appendSecondaryText("  ", SimpleTextAttributes.ERROR_ATTRIBUTES, null)
      .appendSecondaryText("Retry", SimpleTextAttributes.LINK_ATTRIBUTES) { refresh() }

    if (!list.isEmpty) {
      //language=HTML
      val errorText = "<html><body>$prefix<br/>${getLoadingErrorText(error, "<br/>")}<a href=''>Retry</a></body></html>"
      errorPanel.setError(errorText, linkActivationListener = { refresh() })
    }
  }

  private fun getLoadingErrorText(error: Throwable, newLineSeparator: String = "\n"): String {
    if (error is GiteeStatusCodeException && error.error != null) {
      val giteeError = error.error!!
      val builder = StringBuilder(giteeError.message)
      if (giteeError.errors.isNotEmpty()) {
        builder.append(": ").append(newLineSeparator)
        for (e in giteeError.errors) {
          builder.append(e.message ?: "${e.code} error in ${e.resource} field ${e.field}").append(newLineSeparator)
        }
      }
      return builder.toString()
    }

    return error.message?.let { addDotIfNeeded(it) } ?: "Unknown loading error."
  }

  private fun addDotIfNeeded(line: String) = if (line.endsWith('.')) line else "$line."

  override fun dispose() {
    isDisposed = true
  }
}