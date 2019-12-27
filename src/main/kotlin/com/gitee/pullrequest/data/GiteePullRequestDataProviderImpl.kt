// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequests
import com.gitee.api.GiteeGQLRequests
import com.gitee.api.GiteeServerPath
import com.gitee.api.data.pullrequest.GEPullRequestReviewThread
import com.gitee.api.util.GiteeApiPagesLoader
import com.gitee.api.util.SimpleGiteeGQLPagesLoader
import com.gitee.pullrequest.GiteeNotFoundException
import com.gitee.pullrequest.comment.GiteePRCommentsUtil
import com.gitee.pullrequest.data.model.GiteePRDiffReviewThreadMapping
import com.gitee.util.GiteeAsyncUtil
import com.gitee.util.LazyCancellableBackgroundProcessValue
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.Change
import com.intellij.util.EventDispatcher
import git4idea.GitCommit
import git4idea.commands.Git
import git4idea.history.GitCommitRequirements
import git4idea.history.GitLogUtil
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import org.jetbrains.annotations.CalledInAwt
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

internal class GiteePullRequestDataProviderImpl(private val project: Project,
                                                 private val progressManager: ProgressManager,
                                                 private val git: Git,
                                                 private val requestExecutor: GiteeApiRequestExecutor,
                                                 private val repository: GitRepository,
                                                 private val remote: GitRemote,
                                                 private val serverPath: GiteeServerPath,
                                                 private val username: String,
                                                 private val repositoryName: String,
                                                 override val number: Long) : GiteePullRequestDataProvider {

  private val requestsChangesEventDispatcher = EventDispatcher.create(GiteePullRequestDataProvider.RequestsChangedListener::class.java)

  private var lastKnownHeadSha: String? = null

  private val detailsRequestValue = backingValue {
    val details = requestExecutor.execute(it, GiteeApiRequests.Repos.PullRequests.get(serverPath, username, repositoryName, number))
                  ?: throw GiteeNotFoundException("Pull request $number does not exist")

    invokeAndWaitIfNeeded {
      lastKnownHeadSha?.run { if (this != details.head.sha) reloadCommits() }
      lastKnownHeadSha = details.head.sha
    }
    details
  }
  override val detailsRequest
    get() = GiteeAsyncUtil.futureOfMutable { invokeAndWaitIfNeeded { detailsRequestValue.value } }

  private val branchFetchRequestValue = backingValue {
    git.fetch(repository, remote, emptyList(), "refs/pull/${number}/head:").throwOnError()
  }
  override val branchFetchRequest
    get() = GiteeAsyncUtil.futureOfMutable { invokeAndWaitIfNeeded { branchFetchRequestValue.value } }

  private val apiCommitsRequestValue = backingValue {
    GiteeApiPagesLoader.loadAll(requestExecutor, it, GiteeApiRequests.Repos.PullRequests.Commits.pages(serverPath, username, repositoryName, number))

  }
  override val apiCommitsRequest
    get() = GiteeAsyncUtil.futureOfMutable { invokeAndWaitIfNeeded { apiCommitsRequestValue.value } }

  private val logCommitsRequestValue = backingValue<List<GitCommit>> {
    branchFetchRequestValue.value.joinCancellable()
    val commitHashes = apiCommitsRequestValue.value.joinCancellable().map { it.sha }
    val gitCommits = mutableListOf<GitCommit>()
    val requirements = GitCommitRequirements(diffRenameLimit = GitCommitRequirements.DiffRenameLimit.INFINITY,
                                             includeRootChanges = false)
    GitLogUtil.readFullDetailsForHashes(project, repository.root, commitHashes, requirements) {
      gitCommits.add(it)
    }

    gitCommits
  }
  override val logCommitsRequest
    get() = GiteeAsyncUtil.futureOfMutable { invokeAndWaitIfNeeded { logCommitsRequestValue.value } }

  private val diffFileRequestValue = backingValue {
    requestExecutor.execute(it, GiteeApiRequests.Repos.PullRequests.getDiff(serverPath, username, repositoryName, number))
  }
  private val reviewThreadsRequestValue = backingValue {
    SimpleGiteeGQLPagesLoader(requestExecutor, { p ->
      GiteeGQLRequests.PullRequest.reviewThreads(serverPath, username, repositoryName, number, p)
    }).loadAll(it)
  }
  override val reviewThreadsRequest: CompletableFuture<List<GEPullRequestReviewThread>>
    get() = GiteeAsyncUtil.futureOfMutable { invokeAndWaitIfNeeded { reviewThreadsRequestValue.value } }

  private val filesReviewThreadsRequestValue = backingValue {
    GiteePRCommentsUtil.buildThreadsAndMapLines(repository,
                                             logCommitsRequestValue.value.joinCancellable(),
                                             diffFileRequestValue.value.joinCancellable(),
                                             reviewThreadsRequestValue.value.joinCancellable())
  }
  override val filesReviewThreadsRequest: CompletableFuture<Map<Change, List<GiteePRDiffReviewThreadMapping>>>
    get() = GiteeAsyncUtil.futureOfMutable { invokeAndWaitIfNeeded { filesReviewThreadsRequestValue.value } }

  @CalledInAwt
  override fun reloadDetails() {
    detailsRequestValue.drop()
    requestsChangesEventDispatcher.multicaster.detailsRequestChanged()
  }

  @CalledInAwt
  override fun reloadCommits() {
    branchFetchRequestValue.drop()
    apiCommitsRequestValue.drop()
    logCommitsRequestValue.drop()
    requestsChangesEventDispatcher.multicaster.commitsRequestChanged()
    reloadComments()
  }

  @CalledInAwt
  override fun reloadComments() {
    diffFileRequestValue.drop()
    reviewThreadsRequestValue.drop()
    filesReviewThreadsRequestValue.drop()
    requestsChangesEventDispatcher.multicaster.reviewThreadsRequestChanged()
  }

  @Throws(ProcessCanceledException::class)
  private fun <T> CompletableFuture<T>.joinCancellable(): T {
    try {
      return join()
    }
    catch (e: CancellationException) {
      throw ProcessCanceledException(e)
    }
    catch (e: CompletionException) {
      if (GiteeAsyncUtil.isCancellation(e)) throw ProcessCanceledException(e)
      throw e.cause ?: e
    }
  }

  private fun <T> backingValue(supplier: (ProgressIndicator) -> T) =
    object : LazyCancellableBackgroundProcessValue<T>(progressManager) {
      override fun compute(indicator: ProgressIndicator) = supplier(indicator)
    }

  override fun addRequestsChangesListener(listener: GiteePullRequestDataProvider.RequestsChangedListener) =
    requestsChangesEventDispatcher.addListener(listener)

  override fun addRequestsChangesListener(disposable: Disposable, listener: GiteePullRequestDataProvider.RequestsChangedListener) =
    requestsChangesEventDispatcher.addListener(listener, disposable)

  override fun removeRequestsChangesListener(listener: GiteePullRequestDataProvider.RequestsChangedListener) =
    requestsChangesEventDispatcher.removeListener(listener)
}