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
package com.gitee.pullrequest.data

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequests
import com.gitee.api.data.GiteePullRequest
import com.gitee.api.data.GiteePullRequestDetailedWithHtml
import com.gitee.util.GiteeAsyncUtil
import com.gitee.util.NonReusableEmptyProgressIndicator
import com.google.common.cache.CacheBuilder
import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Couple
import com.intellij.openapi.util.LowMemoryWatcher
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.committed.CommittedChangesTreeBrowser
import com.intellij.util.EventDispatcher
import git4idea.GitCommit
import git4idea.commands.Git
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import org.jetbrains.annotations.CalledInAwt
import java.util.*
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException

/**
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/pullrequest/data/GithubPullRequestsDataLoader.kt
 * @author JetBrains s.r.o.
 */
internal class GiteePullRequestsDataLoader(private val project: Project,
                                           private val progressManager: ProgressManager,
                                           private val git: Git,
                                           private val requestExecutor: GiteeApiRequestExecutor,
                                           private val repository: GitRepository,
                                           private val remote: GitRemote) : Disposable {

  private var isDisposed = false
  private val cache = CacheBuilder.newBuilder()
    .removalListener<Long, DataTask> {
      it.value.cancel()
      invalidationEventDispatcher.multicaster.providerChanged(it.key)
    }
    .maximumSize(5)
    .build<Long, DataTask>()

  private val invalidationEventDispatcher = EventDispatcher.create(ProviderChangedListener::class.java)

  init {
    LowMemoryWatcher.register(Runnable { invalidateAllData() }, this)
  }

  @CalledInAwt
  fun invalidateData(number: Long) {
    cache.invalidate(number)
  }

  @CalledInAwt
  fun invalidateAllData() {
    cache.invalidateAll()
  }

  @CalledInAwt
  fun getDataProvider(pullRequest: GiteePullRequest): GiteePullRequestDataProvider {
    if (isDisposed) throw IllegalStateException("Already disposed")

    return cache.get(pullRequest.number) {
      val indicator = NonReusableEmptyProgressIndicator()

      val task = DataTask(pullRequest.url, indicator)
      progressManager.runProcessWithProgressAsynchronously(task, indicator)
      task
    }
  }

  fun addProviderChangesListener(listener: ProviderChangedListener, disposable: Disposable) =
    invalidationEventDispatcher.addListener(listener, disposable)

  private inner class DataTask(private val url: String, private val progressIndicator: ProgressIndicator)
    : Task.Backgroundable(project, "Load Pull Request Data", true), GiteePullRequestDataProvider {

    override val detailsRequest = CompletableFuture<GiteePullRequestDetailedWithHtml>()
    override val branchFetchRequest = CompletableFuture<Couple<String>>()
    override val logCommitsRequest = CompletableFuture<List<GitCommit>>()
    override val changesRequest = CompletableFuture<List<Change>>()

    override fun run(indicator: ProgressIndicator) {
      runPartialTask(detailsRequest, indicator) {
        requestExecutor.execute(progressIndicator, GiteeApiRequests.Repos.PullRequests.getHtml(url))
      }

      runPartialTask(branchFetchRequest, indicator) {
        val details = getOrHandle(detailsRequest)
        git.fetch(repository, remote, emptyList(), "refs/pull/${details.number}/head:").throwOnError()
        if (!isCommitFetched(details.base.sha)) throw IllegalStateException("Pull request base is not available after fetch")
        if (!isCommitFetched(details.head.sha)) throw IllegalStateException("Pull request head is not available after fetch")
        Couple.of(details.base.sha, details.head.sha)
      }

      runPartialTask(logCommitsRequest, indicator) {
        val hashes = getOrHandle(branchFetchRequest)
        GitHistoryUtils.history(project, repository.root, "${hashes.first}..${hashes.second}")
      }

      runPartialTask(changesRequest, indicator) {
        val commits = getOrHandle(logCommitsRequest)
        CommittedChangesTreeBrowser.zipChanges(commits.reversed().flatMap { it.changes })
      }
    }

    private inline fun <T> runPartialTask(resultFuture: CompletableFuture<T>, indicator: ProgressIndicator, crossinline task: () -> T) {
      try {
        if (resultFuture.isCancelled) return
        indicator.checkCanceled()
        val result = task()
        resultFuture.complete(result)
      } catch (pce: ProcessCanceledException) {
        resultFuture.cancel(true)
      } catch (e: Exception) {
        resultFuture.completeExceptionally(e)
      }
    }

    @Throws(ProcessCanceledException::class)
    private fun <T> getOrHandle(future: CompletableFuture<T>): T {
      try {
        return future.join()
      } catch (e: CancellationException) {
        throw ProcessCanceledException(e)
      } catch (e: CompletionException) {
        if (GiteeAsyncUtil.isCancellation(e)) throw ProcessCanceledException(e)
        throw e.cause ?: e
      }
    }

    private fun isCommitFetched(commitHash: String): Boolean {
      val result = git.getObjectType(repository, commitHash)
      return result.success() && result.outputAsJoinedString == "commit"
    }

    internal fun cancel() {
      progressIndicator.cancel()
    }
  }

  override fun dispose() {
    invalidateAllData()
    isDisposed = true
  }

  interface ProviderChangedListener : EventListener {
    fun providerChanged(pullRequestNumber: Long)
  }
}