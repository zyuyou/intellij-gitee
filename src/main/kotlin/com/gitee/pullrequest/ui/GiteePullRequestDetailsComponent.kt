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

import com.gitee.api.data.GiteePullRequestDetailedWithHtml
import com.gitee.pullrequest.avatars.CachingGiteeAvatarIconsProvider
import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.util.ProgressWindow
import com.intellij.openapi.util.Disposer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLoadingPanel
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout

/**
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/pullrequest/ui/GithubPullRequestDetailsComponent.kt
 * @author JetBrains s.r.o.
 */
internal class GiteePullRequestDetailsComponent(iconProviderFactory: CachingGiteeAvatarIconsProvider.Factory)
  : GiteeDataLoadingComponent<GiteePullRequestDetailedWithHtml>(), Disposable {

  private val detailsPanel = GiteePullRequestDetailsPanel(iconProviderFactory)
  private val loadingPanel = JBLoadingPanel(BorderLayout(), this, ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS).apply {
    isOpaque = false
  }

  init {
    isOpaque = true

    detailsPanel.emptyText.text = DEFAULT_EMPTY_TEXT
    loadingPanel.add(detailsPanel)
    setContent(loadingPanel)
    Disposer.register(this, detailsPanel)
  }

  override fun reset() {
    detailsPanel.emptyText.text = DEFAULT_EMPTY_TEXT
    detailsPanel.details = null
  }

  override fun handleResult(result: GiteePullRequestDetailedWithHtml) {
    detailsPanel.details = result
  }

  override fun handleError(error: Throwable) {
    detailsPanel.emptyText
      .clear()
      .appendText("Cannot load details", SimpleTextAttributes.ERROR_ATTRIBUTES)
      .appendSecondaryText(error.message ?: "Unknown error", SimpleTextAttributes.ERROR_ATTRIBUTES, null)
  }

  override fun setBusy(busy: Boolean) {
    if (busy) {
      detailsPanel.emptyText.clear()
      loadingPanel.startLoading()
    }
    else {
      loadingPanel.stopLoading()
    }
  }

  override fun updateUI() {
    super.updateUI()
    background = UIUtil.getListBackground()
  }

  override fun dispose() {}

  companion object {
    //language=HTML
    private const val DEFAULT_EMPTY_TEXT = "Select pull request to view details"
  }
}
