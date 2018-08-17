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
 * Created by zyuyou on 2018/8/10.
 *
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