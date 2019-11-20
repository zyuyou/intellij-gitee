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

import com.gitee.icons.GiteeIcons
import com.gitee.pullrequest.GiteePRToolWindowTabsManager
import com.gitee.util.GitRemoteUrlCoordinates
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository

class GiteeViewPullRequestsAction :
    AbstractGiteeUrlGroupingAction("View Pull Requests", null, GiteeIcons.Gitee_icon) {
  override fun actionPerformed(e: AnActionEvent, project: Project, repository: GitRepository, remote: GitRemote, remoteUrl: String) {
    val remoteCoordinates = GitRemoteUrlCoordinates(remoteUrl, remote, repository)
    project.service<GiteePRToolWindowTabsManager>().showTab(remoteCoordinates)
  }
}