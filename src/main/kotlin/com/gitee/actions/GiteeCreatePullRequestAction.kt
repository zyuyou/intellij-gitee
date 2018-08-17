/*
 * Copyright 2016-2018 码云 - Gitee
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gitee.actions

import com.gitee.api.GiteeApiRequestExecutorManager
import com.gitee.api.GiteeServerPath
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.icons.GiteeIcons
import com.gitee.ui.GiteeCreatePullRequestDialog
import com.gitee.util.GiteeCreatePullRequestWorker
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.openapi.vfs.VirtualFile
import git4idea.DialogManager
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/GithubCreatePullRequestAction.java
 * @author JetBrains s.r.o.
 * @author Aleksey Pivovarov
 */
class GiteeCreatePullRequestAction : LegacySingleAccountActionGroup("Create Pull Request", "Create pull request from current branch", GiteeIcons.Gitee_icon) {
  override fun actionPerformed(project: Project, file: VirtualFile?, gitRepository: GitRepository, account: GiteeAccount) {
    createPullRequest(project, gitRepository, account)
  }

  override fun getRemote(server: GiteeServerPath, repository: GitRepository): Pair<GitRemote, String>? {
    return GiteeCreatePullRequestWorker.findGiteeRemote(server, repository)
  }

  companion object {
    @JvmStatic
    fun createPullRequest(project: Project, gitRepository: GitRepository, account: GiteeAccount) {

      val executor = GiteeApiRequestExecutorManager.getInstance().getExecutor(account, project) ?: return

      val worker = GiteeCreatePullRequestWorker.create(project, gitRepository, executor, account.server) ?: return

      val dialog = GiteeCreatePullRequestDialog(project, worker)

      DialogManager.show(dialog)
    }
  }

}