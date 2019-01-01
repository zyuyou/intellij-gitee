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

package com.gitee.pullrequest.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

/**
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/pullrequest/action/GithubPullRequestRefreshPreviewAction.kt
 * @author JetBrains s.r.o.
 */
class GiteePullRequestRefreshPreviewAction : DumbAwareAction("Refresh Pull Request Details", null, AllIcons.Actions.Refresh) {
  override fun update(e: AnActionEvent) {
    val component = e.getData(GiteePullRequestKeys.PULL_REQUESTS_COMPONENT)
    val selection = e.getData(GiteePullRequestKeys.SELECTED_PULL_REQUEST)
    e.presentation.isEnabled = component != null && selection != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    val selection = e.getRequiredData(GiteePullRequestKeys.SELECTED_PULL_REQUEST)
    e.getRequiredData(GiteePullRequestKeys.PULL_REQUESTS_COMPONENT).refreshPullRequest(selection.number)
  }
}