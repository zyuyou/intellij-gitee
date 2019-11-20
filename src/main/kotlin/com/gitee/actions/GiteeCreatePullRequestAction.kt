// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.actions

import com.gitee.GiteeCreatePullRequestWorker
import com.gitee.api.GiteeApiRequestExecutorManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.icons.GiteeIcons
import com.gitee.ui.GiteeCreatePullRequestDialog
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import git4idea.DialogManager
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository

/**
 * @author Aleksey Pivovarov
 */
class GiteeCreatePullRequestAction : AbstractAuthenticatingGiteeUrlGroupingAction("Create Pull Request", "Create pull request from current branch", GiteeIcons.Gitee_icon) {

  override fun actionPerformed(e: AnActionEvent,
                               project: Project,
                               repository: GitRepository,
                               remote: GitRemote,
                               remoteUrl: String,
                               account: GiteeAccount) {
    createPullRequest(project, repository, remote, remoteUrl, account)
  }

  companion object {

    internal fun createPullRequest(project: Project,
                                   gitRepository: GitRepository,
                                   remote: GitRemote,
                                   remoteUrl: String,
                                   account: GiteeAccount) {
      val executor = GiteeApiRequestExecutorManager.getInstance().getExecutor(account, project) ?: return

      val worker = GiteeCreatePullRequestWorker.create(project, gitRepository, remote, remoteUrl,
          executor, account.server)
          ?: return

      val dialog = GiteeCreatePullRequestDialog(project, worker)
      DialogManager.show(dialog)
    }
  }
}