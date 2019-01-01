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

import com.gitee.api.*
import com.gitee.api.data.GiteeRepoDetailed
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.icons.GiteeIcons
import com.gitee.util.GiteeGitHelper
import com.gitee.util.GiteeNotifications
import com.gitee.util.GiteeUrlUtil
import com.gitee.util.GiteeUtil
import com.intellij.dvcs.DvcsUtil
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsException
import git4idea.GitUtil
import git4idea.actions.BasicAction
import git4idea.commands.*
import git4idea.commands.GitLocalChangesWouldBeOverwrittenDetector.Operation.CHECKOUT
import git4idea.config.GitVcsSettings
import git4idea.fetch.GitFetchSupport.fetchSupport
import git4idea.rebase.GitRebaseProblemDetector
import git4idea.rebase.GitRebaser
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import git4idea.update.GitUpdateResult
import git4idea.util.GitPreservingProcess
import java.io.IOException

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/GithubRebaseAction.java
 * @author JetBrains s.r.o.
 */
class GiteeRebaseAction : AbstractGiteeUrlGroupingAction(
  "Rebase my Gitee fork",
  "Rebase your Gitee forked repository relative to the origin",
  GiteeIcons.Gitee_icon) {

  override fun actionPerformed(e: AnActionEvent,
                               project: Project,
                               repository: GitRepository,
                               remote: GitRemote,
                               remoteUrl: String,
                               account: GiteeAccount) {
    BasicAction.saveAll()

    val executor = GiteeApiRequestExecutorManager.getInstance().getExecutor(account, project) ?: return

    val repoPath = GiteeUrlUtil.getUserAndRepositoryFromRemoteUrl(remoteUrl)

    if (repoPath == null) {
      GiteeNotifications.showError(project, CANNOT_PERFORM_GITEE_REBASE, "Invalid Gitee remote: $remoteUrl")
      return
    }
    RebaseTask(project, executor, Git.getInstance(), account.server, repository, repoPath, "upstream/master").queue()
  }

  companion object {
    private val LOG = GiteeUtil.LOG
    private const val CANNOT_PERFORM_GITEE_REBASE = "Can't perform Gitee rebase"
  }

  private class RebaseTask(project: Project,
                           private val requestExecutor: GiteeApiRequestExecutor,
                           private val git: Git,
                           private val server: GiteeServerPath,
                           private val repository: GitRepository,
                           private val repoPath: GiteeFullPath,
                           private val onto: String) : Task.Backgroundable(project, "Rebasing Gitee Fork...") {

    override fun run(indicator: ProgressIndicator) {
      repository.update()

      var upstreamRemoteUrl = findUpstreamRemoteUrl()

      if (upstreamRemoteUrl == null) {
        indicator.text = "Configuring upstream remote..."
        LOG.info("Configuring upstream remote")
        upstreamRemoteUrl = configureUpstreamRemote(indicator) ?: return
      }

      if (isUpstreamWithSameUsername(indicator, upstreamRemoteUrl)) {
        GiteeNotifications.showError(myProject, CANNOT_PERFORM_GITEE_REBASE,
          "Configured upstream seems to be your own repository: $upstreamRemoteUrl")
        return
      }

      LOG.info("Fetching upstream")
      indicator.text = "Fetching upstream..."
      if (!fetchParent(indicator)) {
        return
      }

      LOG.info("Rebasing current branch")
      indicator.text = "Rebasing current branch..."
      rebaseCurrentBranch(indicator)
    }

    private fun findUpstreamRemoteUrl(): String? {
      return repository.remotes.stream()
        .filter { remote ->
          remote.name == "upstream" &&
            remote.firstUrl != null &&
            server.matches(remote.firstUrl!!)
        }
        .findFirst()
        .map(GitRemote::getFirstUrl).orElse(null)
    }


    private fun isUpstreamWithSameUsername(indicator: ProgressIndicator, upstreamRemoteUrl: String): Boolean {
      try {
        val userAndRepo = GiteeUrlUtil.getUserAndRepositoryFromRemoteUrl(upstreamRemoteUrl)
        if (userAndRepo == null) {
          GiteeNotifications.showError(myProject, CANNOT_PERFORM_GITEE_REBASE, "Can't validate upstream remote: $upstreamRemoteUrl")
          return true
        }
        val username = requestExecutor.execute(indicator, GiteeApiRequests.CurrentUser.get(server)).login
        return userAndRepo.user == username
      } catch (e: IOException) {
        GiteeNotifications.showError(myProject, CANNOT_PERFORM_GITEE_REBASE, "Can't get user information")
        return true
      }

    }

    private fun configureUpstreamRemote(indicator: ProgressIndicator): String? {
      val repositoryInfo = loadRepositoryInfo(indicator, repoPath) ?: return null

      if (!repositoryInfo.isFork || repositoryInfo.parent == null) {
        GiteeNotifications
          .showWarningURL(myProject, CANNOT_PERFORM_GITEE_REBASE, "Gitee repository ", "'" + repositoryInfo.name + "'",
            " is not a fork", repositoryInfo.htmlUrl)
        return null
      }

      val parentRepoUrl = GiteeGitHelper.getInstance().getRemoteUrl(server, repositoryInfo.parent!!.fullPath)

      LOG.info("Adding Gitee parent as a remote host")
      indicator.text = "Adding Gitee parent as a remote host..."
      try {
        git.addRemote(repository, "upstream", parentRepoUrl).throwOnError()
      } catch (e: VcsException) {
        GiteeNotifications
          .showError(myProject, CANNOT_PERFORM_GITEE_REBASE, "Could not configure \"upstream\" remote:\n" + e.message)
        return null
      }

      repository.update()
      return parentRepoUrl
    }

    private fun loadRepositoryInfo(indicator: ProgressIndicator, fullPath: GiteeFullPath): GiteeRepoDetailed? {
      return try {
        val repo = requestExecutor.execute(indicator, GiteeApiRequests.Repos.get(server, fullPath.user, fullPath.repository))
        if (repo == null) GiteeNotifications.showError(myProject, "Repository " + fullPath.toString() + " was not found", "")
        repo
      } catch (e: IOException) {
        GiteeNotifications.showError(myProject, "Can't load repository info", e)
        null
      }

    }

    private fun fetchParent(indicator: ProgressIndicator): Boolean {
      val remote = GitUtil.findRemoteByName(repository, "upstream")
      if (remote == null) {
        LOG.warn("Couldn't find remote  remoteName  in $repository")
        return false
      }
      return fetchSupport(myProject).fetch(repository, remote).showNotificationIfFailed()

    }

    private fun rebaseCurrentBranch(indicator: ProgressIndicator) {
      DvcsUtil.workingTreeChangeStarted(myProject, "Rebase").use { _ ->
        val rootsToSave = listOf(repository.root)

        val process = GitPreservingProcess(myProject, git, rootsToSave, "Rebasing", onto,
          GitVcsSettings.UpdateChangesPolicy.STASH, indicator
        ) {
          doRebaseCurrentBranch(indicator)
        }

        process.execute()
      }
    }

    private fun doRebaseCurrentBranch(indicator: ProgressIndicator) {
      val repositoryManager = GitUtil.getRepositoryManager(myProject)
      val rebaser = GitRebaser(myProject, git, indicator)
      val root = repository.root

      val handler = GitLineHandler(myProject, root, GitCommand.REBASE)
      handler.setStdoutSuppressed(false)
      handler.addParameters(onto)

      val rebaseConflictDetector = GitRebaseProblemDetector()
      handler.addLineListener(rebaseConflictDetector)

      val untrackedFilesDetector = GitUntrackedFilesOverwrittenByOperationDetector(root)
      val localChangesDetector = GitLocalChangesWouldBeOverwrittenDetector(root, CHECKOUT)
      handler.addLineListener(untrackedFilesDetector)
      handler.addLineListener(localChangesDetector)
      handler.addLineListener(GitStandardProgressAnalyzer.createListener(indicator))

      val oldText = indicator.text
      indicator.text = "Rebasing onto $onto..."
      val rebaseResult = git.runCommand(handler)
      indicator.text = oldText
      repositoryManager.updateRepository(root)

      if (rebaseResult.success()) {
        root.refresh(false, true)
        GiteeNotifications.showInfo(myProject, "Success", "Successfully rebased Gitee fork")
      } else {
        val result = rebaser.handleRebaseFailure(handler, root, rebaseResult, rebaseConflictDetector, untrackedFilesDetector, localChangesDetector)

        if (result == GitUpdateResult.NOTHING_TO_UPDATE ||
          result == GitUpdateResult.SUCCESS ||
          result == GitUpdateResult.SUCCESS_WITH_RESOLVED_CONFLICTS) {
          GiteeNotifications.showInfo(myProject, "Success", "Successfully rebased Gitee fork")
        }
      }
    }
  }

}