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
package com.gitee.actions

import com.gitee.api.GiteeApiRequestExecutorManager
import com.gitee.api.GiteeApiRequests
import com.gitee.api.data.GiteeRepoDetailed
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.icons.GiteeIcons
import com.gitee.pullrequest.GiteePullRequestsToolWindowManager
import com.gitee.util.GiteeNotifications
import com.gitee.util.GiteeUrlUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository

class GiteeViewPullRequestsAction : AbstractGiteeUrlGroupingAction("View Pull Requests", null, GiteeIcons.Gitee_icon) {

  override fun actionPerformed(e: AnActionEvent,
                               project: Project,
                               repository: GitRepository,
                               remote: GitRemote,
                               remoteUrl: String,
                               account: GiteeAccount) {

    val fullPath = GiteeUrlUtil.getUserAndRepositoryFromRemoteUrl(remoteUrl)

    if (fullPath == null) {
      GiteeNotifications.showError(project, "Invalid Gitee Repository URL", "$remoteUrl is not a Gitee repository.")
      return
    }

    val requestExecutor = service<GiteeApiRequestExecutorManager>().getExecutor(account, project) ?: return

    val toolWindowManager = project.service<GiteePullRequestsToolWindowManager>()

    if (toolWindowManager.showPullRequestsTabIfExists(repository, remote, remoteUrl, account)) return

    object : Task.Backgroundable(project, "Loading Gitee Repository Information", true, PerformInBackgroundOption.DEAF) {
      lateinit var repoDetails: GiteeRepoDetailed

      override fun run(indicator: ProgressIndicator) {
        val details = requestExecutor.execute(indicator, GiteeApiRequests.Repos.get(account.server, fullPath.user, fullPath.repository))
                      ?: throw IllegalArgumentException("Repository $fullPath does not exist at ${account.server} or you don't have access.")

        repoDetails = details
        indicator.checkCanceled()
      }

      override fun onSuccess() {
        toolWindowManager.createPullRequestsTab(requestExecutor, repository, remote, remoteUrl, repoDetails, account)
        toolWindowManager.showPullRequestsTabIfExists(repository, remote, remoteUrl, account)
      }

      override fun onThrowable(error: Throwable) {
        GiteeNotifications.showError(project, "Failed To Load Repository Information", error)
      }
    }.queue()
  }
}