// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data.service

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequests
import com.gitee.api.GiteeRepositoryPath
import com.gitee.api.GiteeServerPath
import com.gitee.api.data.GiteeCommit
import com.gitee.api.data.GiteeIssueState
import com.gitee.api.data.GiteePullRequestDetailed
import com.gitee.pullrequest.action.ui.GiteeMergeCommitMessageDialog
import com.gitee.pullrequest.data.GiteePullRequestsBusyStateTracker
import com.gitee.pullrequest.data.GiteePullRequestsDataContext.Companion.PULL_REQUEST_EDITED_TOPIC
import com.gitee.pullrequest.data.GiteePullRequestsDataLoader
import com.gitee.util.GiteeAsyncUtil
import com.gitee.util.GiteeNotifications
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.messages.MessageBus
import org.jetbrains.annotations.CalledInAwt

class GiteePullRequestsStateServiceImpl internal constructor(private val project: Project,
                                                             private val progressManager: ProgressManager,
                                                             private val messageBus: MessageBus,
                                                             private val dataLoader: GiteePullRequestsDataLoader,
                                                             private val busyStateTracker: GiteePullRequestsBusyStateTracker,
                                                             private val requestExecutor: GiteeApiRequestExecutor,
                                                             private val serverPath: GiteeServerPath,
                                                             private val repoPath: GiteeRepositoryPath)
  : GiteePullRequestsStateService {

  @CalledInAwt
  override fun close(pullRequest: Long) {
    if (!busyStateTracker.acquire(pullRequest)) return

    progressManager.run(object : Task.Backgroundable(project, "Closing Pull Request...", true) {
      override fun run(indicator: ProgressIndicator) {
        requestExecutor.execute(indicator,
                                GiteeApiRequests.Repos.PullRequests.update(serverPath, repoPath.owner, repoPath.repository, pullRequest,
                                                                            state = GiteeIssueState.closed))
        messageBus.syncPublisher(PULL_REQUEST_EDITED_TOPIC).onPullRequestEdited(pullRequest)
      }

      override fun onSuccess() {
        GiteeNotifications.showInfo(project, "Pull Request Closed", "Successfully closed pull request #${pullRequest}")
      }

      override fun onThrowable(error: Throwable) {
        GiteeNotifications.showError(project, "Failed To Close Pull Request", error)
      }

      override fun onFinished() {
        busyStateTracker.release(pullRequest)
      }
    })
  }

  @CalledInAwt
  override fun reopen(pullRequest: Long) {
    if (!busyStateTracker.acquire(pullRequest)) return

    progressManager.run(object : Task.Backgroundable(project, "Reopening Pull Request...", true) {
      override fun run(indicator: ProgressIndicator) {
        requestExecutor.execute(indicator,
                                GiteeApiRequests.Repos.PullRequests.update(serverPath, repoPath.owner, repoPath.repository, pullRequest,
                                                                            state = GiteeIssueState.open))
        messageBus.syncPublisher(PULL_REQUEST_EDITED_TOPIC).onPullRequestEdited(pullRequest)
      }

      override fun onSuccess() {
        GiteeNotifications.showInfo(project, "Pull Request Reopened", "Successfully reopened pull request #${pullRequest}")
      }

      override fun onThrowable(error: Throwable) {
        GiteeNotifications.showError(project, "Failed To Reopen Pull Request", error)
      }

      override fun onFinished() {
        busyStateTracker.release(pullRequest)
      }
    })
  }

  @CalledInAwt
  override fun merge(pullRequest: Long) {
    if (!busyStateTracker.acquire(pullRequest)) return

    val dataProvider = dataLoader.getDataProvider(pullRequest)
    progressManager.run(object : Task.Backgroundable(project, "Merging Pull Request...", true) {
      private lateinit var details: GiteePullRequestDetailed

      override fun run(indicator: ProgressIndicator) {
        indicator.text2 = "Loading details"
        details = GiteeAsyncUtil.awaitFuture(indicator, dataProvider.detailsRequest)
        indicator.checkCanceled()

        indicator.text2 = "Acquiring commit message"
        val commitMessage = invokeAndWaitIfNeeded {
          val dialog = GiteeMergeCommitMessageDialog(project,
                                                      "Merge Pull Request",
                                                      "Merge pull request #${pullRequest}",
                                                      details.title)
          if (dialog.showAndGet()) dialog.message else null
        } ?: throw ProcessCanceledException()

        indicator.text2 = "Merging"
        requestExecutor.execute(indicator, GiteeApiRequests.Repos.PullRequests.merge(serverPath, repoPath, details.number,
                                                                                      commitMessage.first, commitMessage.second,
                                                                                      details.head.sha))
        messageBus.syncPublisher(PULL_REQUEST_EDITED_TOPIC).onPullRequestEdited(pullRequest)
      }

      override fun onSuccess() {
        GiteeNotifications.showInfo(project, "Pull Request Merged", "Successfully merged pull request #${details.number}")
      }

      override fun onThrowable(error: Throwable) {
        GiteeNotifications.showError(project, "Failed To Merge Pull Request", error)
      }

      override fun onFinished() {
        busyStateTracker.release(pullRequest)
      }
    })
  }

  @CalledInAwt
  override fun rebaseMerge(pullRequest: Long) {
    if (!busyStateTracker.acquire(pullRequest)) return

    val dataProvider = dataLoader.getDataProvider(pullRequest)
    progressManager.run(object : Task.Backgroundable(project, "Merging Pull Request...", true) {
      private lateinit var details: GiteePullRequestDetailed

      override fun run(indicator: ProgressIndicator) {
        indicator.text2 = "Loading details"
        details = GiteeAsyncUtil.awaitFuture(indicator, dataProvider.detailsRequest)
        indicator.checkCanceled()

        indicator.text2 = "Merging"
        requestExecutor.execute(indicator, GiteeApiRequests.Repos.PullRequests.rebaseMerge(serverPath, repoPath, details.number,
                                                                                            details.head.sha))
        messageBus.syncPublisher(PULL_REQUEST_EDITED_TOPIC).onPullRequestEdited(pullRequest)
      }

      override fun onSuccess() {
        GiteeNotifications.showInfo(project, "Pull Request Rebased and Merged",
                                     "Successfully rebased and merged pull request #${details.number}")
      }

      override fun onThrowable(error: Throwable) {
        GiteeNotifications.showError(project, "Failed To Rebase and Merge Pull Request", error)
      }

      override fun onFinished() {
        busyStateTracker.release(pullRequest)
      }
    })
  }

  @CalledInAwt
  override fun squashMerge(pullRequest: Long) {
    if (!busyStateTracker.acquire(pullRequest)) return

    val dataProvider = dataLoader.getDataProvider(pullRequest)
    progressManager.run(object : Task.Backgroundable(project, "Merging Pull Request...", true) {
      private lateinit var details: GiteePullRequestDetailed
      private lateinit var commits: List<GiteeCommit>


      override fun run(indicator: ProgressIndicator) {
        indicator.text2 = "Loading details"
        details = GiteeAsyncUtil.awaitFuture(indicator, dataProvider.detailsRequest)
        indicator.checkCanceled()

        indicator.text2 = "Loading commits"
        commits = GiteeAsyncUtil.awaitFuture(indicator, dataProvider.apiCommitsRequest)
        indicator.checkCanceled()

        indicator.text2 = "Acquiring commit message"
        val body = "* " + StringUtil.join(commits, { it.commit.message }, "\n\n* ")
        val commitMessage = invokeAndWaitIfNeeded {
          val dialog = GiteeMergeCommitMessageDialog(project,
                                                      "Merge Pull Request",
                                                      "Merge pull request #${pullRequest}",
                                                      body)
          if (dialog.showAndGet()) dialog.message else null
        } ?: throw ProcessCanceledException()

        indicator.text2 = "Merging"
        requestExecutor.execute(indicator, GiteeApiRequests.Repos.PullRequests.squashMerge(serverPath, repoPath, details.number,
                                                                                            commitMessage.first, commitMessage.second,
                                                                                            details.head.sha))
        messageBus.syncPublisher(PULL_REQUEST_EDITED_TOPIC).onPullRequestEdited(pullRequest)
      }

      override fun onSuccess() {
        GiteeNotifications.showInfo(project, "Pull Request Squashed and Merged",
                                     "Successfully squashed amd merged pull request #${details.number}")
      }

      override fun onThrowable(error: Throwable) {
        GiteeNotifications.showError(project, "Failed To Squash and Merge Pull Request", error)
      }

      override fun onFinished() {
        busyStateTracker.release(pullRequest)
      }
    })
  }
}
