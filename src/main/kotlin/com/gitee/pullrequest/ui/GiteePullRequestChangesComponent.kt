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

import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.util.ProgressWindow
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ui.ChangesBrowserBase
import com.intellij.openapi.vcs.changes.ui.TreeModelBuilder
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.SideBorder
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.util.ui.ComponentWithEmptyText
import java.awt.BorderLayout
import javax.swing.border.Border
import kotlin.properties.Delegates

/**
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/pullrequest/ui/GithubPullRequestChangesComponent.kt
 * @author JetBrains s.r.o.
 */
internal class GiteePullRequestChangesComponent(project: Project) : GiteeDataLoadingComponent<List<Change>>(), Disposable {

  private val changesBrowser = PullRequestChangesBrowserWithError(project)
  private val loadingPanel = JBLoadingPanel(BorderLayout(), this, ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS)

  val diffAction = changesBrowser.diffAction

  init {
    loadingPanel.add(changesBrowser, BorderLayout.CENTER)
    changesBrowser.emptyText.text = DEFAULT_EMPTY_TEXT
    setContent(loadingPanel)
  }

  override fun reset() {
    changesBrowser.emptyText.text = DEFAULT_EMPTY_TEXT
    changesBrowser.changes = emptyList()
  }

  override fun handleResult(result: List<Change>) {
    changesBrowser.emptyText.text = "Pull request does not contain any changes"
    changesBrowser.changes = result
  }

  override fun handleError(error: Throwable) {
    changesBrowser.emptyText
      .clear()
      .appendText("Cannot load changes", SimpleTextAttributes.ERROR_ATTRIBUTES)
      .appendSecondaryText(error.message ?: "Unknown error", SimpleTextAttributes.ERROR_ATTRIBUTES, null)
  }

  override fun setBusy(busy: Boolean) {
    if (busy) {
      changesBrowser.emptyText.clear()
      loadingPanel.startLoading()
    }
    else {
      loadingPanel.stopLoading()
    }
  }

  override fun dispose() {}

  companion object {
    //language=HTML
    private const val DEFAULT_EMPTY_TEXT = "Select pull request to view list of changed files"

    private class PullRequestChangesBrowserWithError(project: Project)
      : ChangesBrowserBase(project, false, false), ComponentWithEmptyText {

      var changes: List<Change> by Delegates.observable(listOf()) { _, _, _ ->
        myViewer.rebuildTree()
      }

      init {
        init()
      }

      override fun buildTreeModel() = TreeModelBuilder.buildFromChanges(myProject, grouping, changes, null)

      override fun onDoubleClick() {
        if (canShowDiff()) super.onDoubleClick()
      }

      override fun getEmptyText() = myViewer.emptyText

      override fun createViewerBorder(): Border = IdeBorderFactory.createBorder(SideBorder.TOP)
    }
  }
}