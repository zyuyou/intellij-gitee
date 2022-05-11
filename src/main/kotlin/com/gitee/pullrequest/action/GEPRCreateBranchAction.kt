// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.gitee.pullrequest.action

import com.gitee.api.data.GiteePullRequestDetailed
import com.gitee.i18n.GiteeBundle
import com.gitee.pullrequest.data.provider.GEPRDataProvider
import com.gitee.util.GiteeGitHelper
import com.gitee.util.GiteeNotificationIdsHolder
import com.gitee.util.GiteeSettings
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.util.ProgressIndicatorUtils
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.VcsNotifier
import git4idea.branch.GitBranchUiHandlerImpl
import git4idea.branch.GitBranchUtil
import git4idea.branch.GitBranchWorker
import git4idea.commands.Git
import git4idea.fetch.GitFetchSupport
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository

class GEPRCreateBranchAction : DumbAwareAction(GiteeBundle.messagePointer("pull.request.branch.checkout.create.action"),
                                               GiteeBundle.messagePointer("pull.request.branch.checkout.create.action.description"),
                                               null) {

  override fun update(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT)
    val repository = e.getData(GEPRActionKeys.GIT_REPOSITORY)
    val selection = e.getData(GEPRActionKeys.PULL_REQUEST_DATA_PROVIDER)
    if (repository != null) {
      val loadedDetails = selection?.detailsData?.loadedDetails
      val headRefName = loadedDetails?.headRefName
      val httpUrl = loadedDetails?.headRepository?.url
      val sshUrl = loadedDetails?.headRepository?.sshUrl
      val isFork = loadedDetails?.headRepository?.isFork ?: false

      val remote = GiteeGitHelper.getInstance().findRemote(repository, httpUrl, sshUrl)
      if (remote != null) {
        val localBranch = GiteeGitHelper.getInstance().findLocalBranch(repository, remote, isFork, headRefName)
        if (repository.currentBranchName == localBranch) {
          e.presentation.isEnabled = false
          return
        }
      }
    }
    e.presentation.isEnabled = project != null && !project.isDefault && selection != null && repository != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getRequiredData(CommonDataKeys.PROJECT)
    val repository = e.getRequiredData(GEPRActionKeys.GIT_REPOSITORY)
    val dataProvider = e.getRequiredData(GEPRActionKeys.PULL_REQUEST_DATA_PROVIDER)

    val pullRequestNumber = dataProvider.id.number
    checkoutOrCreateNew(project, repository, pullRequestNumber, dataProvider)
  }

  private fun checkoutOrCreateNew(project: Project, repository: GitRepository, pullRequestNumber: Long, dataProvider: GEPRDataProvider) {
    val httpForkUrl = dataProvider.httpForkUrl
    val sshForkUrl = dataProvider.sshForkUrl
//    val possibleBranchName = dataProvider.detailsData.loadedDetails?.headRefName
    val possibleBranchName = dataProvider.detailsData.loadedDetails?.head?.ref
    val existingRemote = GiteeGitHelper.getInstance().findRemote(repository, httpForkUrl, sshForkUrl)
    if (existingRemote != null) {
      val localBranch = GiteeGitHelper.getInstance().findLocalBranch(repository, existingRemote, dataProvider.isFork, possibleBranchName)
      if (localBranch != null) {
        checkoutBranch(project, repository, localBranch, dataProvider)
        return
      }
    }
    checkoutNewBranch(project, repository, pullRequestNumber, dataProvider)
  }

  private fun checkoutBranch(project: Project, repository: GitRepository, localBranch: String, dataProvider: GEPRDataProvider) {
    object : Task.Backgroundable(project, GiteeBundle.message("pull.request.branch.checkout.task.title"), true) {
      private val git = Git.getInstance()

      override fun run(indicator: ProgressIndicator) {
        ProgressIndicatorUtils.awaitWithCheckCanceled(dataProvider.changesData.fetchHeadBranch(), indicator)

        indicator.text = GiteeBundle.message("pull.request.branch.checkout.task.indicator")
        GitBranchWorker(project, git, GitBranchUiHandlerImpl(project, indicator))
          .checkout(localBranch, false, listOf(repository))
      }
    }.queue()
  }

  private fun checkoutNewBranch(project: Project, repository: GitRepository, pullRequestNumber: Long, dataProvider: GEPRDataProvider) {
    val options = GitBranchUtil.getNewBranchNameFromUser(project, listOf(repository),
                                                         GiteeBundle.message("pull.request.branch.checkout.create.dialog.title",
                                                                              pullRequestNumber),
                                                         generateSuggestedBranchName(repository, pullRequestNumber,
                                                                                     dataProvider), true) ?: return

    if (!options.checkout) {
      object : Task.Backgroundable(project, GiteeBundle.message("pull.request.branch.checkout.create.task.title"), true) {
        private val git = Git.getInstance()

        override fun run(indicator: ProgressIndicator) {
          val gePullRequest = ProgressIndicatorUtils.awaitWithCheckCanceled(dataProvider.detailsData.loadDetails(), indicator)
          val sha = gePullRequest.headRefOid
          ProgressIndicatorUtils.awaitWithCheckCanceled(dataProvider.changesData.fetchHeadBranch(), indicator)

          indicator.text = GiteeBundle.message("pull.request.branch.checkout.create.task.indicator")
          GitBranchWorker(project, git, GitBranchUiHandlerImpl(project, indicator))
            .createBranch(options.name, mapOf(repository to sha))
          if (options.setTracking) {
            trySetTrackingUpstreamBranch(git, repository, dataProvider, options.name, gePullRequest)
          }
          repository.update()
        }
      }.queue()
    }
    else {
      object : Task.Backgroundable(project, GiteeBundle.message("pull.request.branch.checkout.task.title"), true) {
        private val git = Git.getInstance()

        override fun run(indicator: ProgressIndicator) {
          val gePullRequest = ProgressIndicatorUtils.awaitWithCheckCanceled(dataProvider.detailsData.loadDetails(), indicator)
          val sha = gePullRequest.headRefOid
          ProgressIndicatorUtils.awaitWithCheckCanceled(dataProvider.changesData.fetchHeadBranch(), indicator)

          indicator.text = GiteeBundle.message("pull.request.branch.checkout.task.indicator")
          GitBranchWorker(project, git, GitBranchUiHandlerImpl(project, indicator))
            .checkoutNewBranchStartingFrom(options.name, sha, listOf(repository))
          if (options.setTracking) {
            trySetTrackingUpstreamBranch(git, repository, dataProvider, options.name, gePullRequest)
          }
          repository.update()
        }
      }.queue()
    }
  }

  private fun generateSuggestedBranchName(repository: GitRepository, pullRequestNumber: Long, dataProvider: GEPRDataProvider): String =
    dataProvider.detailsData.loadedDetails.let { gePullRequest ->
      val login = gePullRequest?.headRepository?.owner?.login
      val headRefName = gePullRequest?.headRefName
      when {
        headRefName == null || login == null -> "pull/${pullRequestNumber}"
        repository.branchWithTrackingExist(headRefName) -> "${login}_${headRefName}"
        else -> headRefName
      }
    }

  private fun GitRepository.branchWithTrackingExist(branchName: String) =
    branches.findLocalBranch(branchName)?.findTrackedBranch(this) != null

  private fun trySetTrackingUpstreamBranch(git: Git,
                                           repository: GitRepository,
                                           dataProvider: GEPRDataProvider,
                                           branchName: String,
                                           gePullRequest: GiteePullRequestDetailed
  ) {
    val project = repository.project
    val vcsNotifier = project.service<VcsNotifier>()
    val pullRequestAuthor = gePullRequest.author
    if (pullRequestAuthor == null) {
      vcsNotifier.notifyError(
        GiteeNotificationIdsHolder.PULL_REQUEST_CANNOT_SET_TRACKING_BRANCH,
                              GiteeBundle.message("pull.request.branch.checkout.set.tracking.branch.failed"),
                              GiteeBundle.message("pull.request.branch.checkout.resolve.author.failed"))
      return
    }
    val httpForkUrl = dataProvider.httpForkUrl
    val sshForkUrl = dataProvider.sshForkUrl
    val forkRemote = git.findOrCreateRemote(repository, pullRequestAuthor.login, httpForkUrl, sshForkUrl)

    if (forkRemote == null) {
      var failedMessage = GiteeBundle.message("pull.request.branch.checkout.resolve.remote.failed")
      if (httpForkUrl != null) {
        failedMessage += "\n$httpForkUrl"
      }
      if (sshForkUrl != null) {
        failedMessage += "\n$sshForkUrl"
      }
      vcsNotifier.notifyError(GiteeNotificationIdsHolder.PULL_REQUEST_CANNOT_SET_TRACKING_BRANCH,
                              GiteeBundle.message("pull.request.branch.checkout.set.tracking.branch.failed"), failedMessage)
      return
    }

//    val forkBranchName = "${forkRemote.name}/${gePullRequest.headRefName}"
    val forkBranchName = "${forkRemote.name}/${gePullRequest.headRefName}"
    val fetchResult = GitFetchSupport.fetchSupport(project).fetch(repository, forkRemote, gePullRequest.headRefName)
    if (fetchResult.showNotificationIfFailed(GiteeBundle.message("pull.request.branch.checkout.set.tracking.branch.failed"))) {
      val setUpstream = git.setUpstream(repository, forkBranchName, branchName)
      if (!setUpstream.success()) {
        vcsNotifier.notifyError(GiteeNotificationIdsHolder.PULL_REQUEST_CANNOT_SET_TRACKING_BRANCH,
                                GiteeBundle.message("pull.request.branch.checkout.set.tracking.branch.failed"),
                                setUpstream.errorOutputAsHtmlString)
      }
    }
  }

  private fun Git.findOrCreateRemote(repository: GitRepository, remoteName: String, httpUrl: String?, sshUrl: String?): GitRemote? {
    val existingRemote = GiteeGitHelper.getInstance().findRemote(repository, httpUrl, sshUrl)
    if (existingRemote != null) return existingRemote

    val useSshUrl = GiteeSettings.getInstance().isCloneGitUsingSsh
    val sshOrHttpUrl = if (useSshUrl) sshUrl else httpUrl

    if (sshOrHttpUrl != null && repository.remotes.any { it.name == remoteName }) {
      return createRemote(repository, "pull_$remoteName", sshOrHttpUrl)
    }

    return when {
      useSshUrl && sshUrl != null -> createRemote(repository, remoteName, sshUrl)
      !useSshUrl && httpUrl != null -> createRemote(repository, remoteName, httpUrl)
      sshUrl != null -> createRemote(repository, remoteName, sshUrl)
      httpUrl != null -> createRemote(repository, remoteName, httpUrl)
      else -> null
    }
  }

  private fun Git.createRemote(repository: GitRepository, remoteName: String, url: String): GitRemote? =
    with(repository) {
      addRemote(this, remoteName, url)
      update()
      remotes.find { it.name == remoteName }
    }

  private val GEPRDataProvider.isFork get() = detailsData.loadedDetails?.headRepository?.isFork ?: false
  private val GEPRDataProvider.httpForkUrl get() = detailsData.loadedDetails?.headRepository?.url
  private val GEPRDataProvider.sshForkUrl get() = detailsData.loadedDetails?.headRepository?.sshUrl
}
