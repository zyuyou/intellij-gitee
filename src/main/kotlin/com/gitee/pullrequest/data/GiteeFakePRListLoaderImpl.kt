// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequests
import com.gitee.api.GiteeRepositoryPath
import com.gitee.api.GiteeServerPath
import com.gitee.api.data.GiteePullRequest
import com.gitee.api.data.request.GiteeRequestPagination
import com.gitee.api.util.GiteeApiUrlQueryBuilder
import com.gitee.api.util.SimpleGiteeFakeGQLPagesLoader
import com.gitee.pullrequest.search.GiteePullRequestSearchQuery
import com.gitee.pullrequest.search.GiteePullRequestSearchQueryHolder
import com.gitee.pullrequest.ui.SimpleEventListener
import com.gitee.util.NonReusableEmptyProgressIndicator
import com.gitee.util.handleOnEdt
import com.intellij.concurrency.JobScheduler
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.CollectionListModel
import com.intellij.util.EventDispatcher
import org.jetbrains.annotations.CalledInAwt
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

internal class GiteeFakePRListLoaderImpl(progressManager: ProgressManager,
                                         private val requestExecutor: GiteeApiRequestExecutor,
                                         private val serverPath: GiteeServerPath,
                                         private val repoPath: GiteeRepositoryPath,
                                         private val listModel: CollectionListModel<GiteePullRequest>,
                                         private val searchQueryHolder: GiteePullRequestSearchQueryHolder)
  : GiteeFakeGQLPagedListLoader<GiteePullRequest>(progressManager,
  SimpleGiteeFakeGQLPagesLoader(requestExecutor, { p ->
    GiteeApiRequests.Repos.PullRequests.search(serverPath, repoPath, buildQuery(searchQueryHolder.query, p))
  })),
  GiteeFakePRListLoader {

  override val hasLoadedItems: Boolean
    get() = !listModel.isEmpty

  private val outdatedStateEventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)

  override var outdated: Boolean by Delegates.observable(false) { _, _, newValue ->
    if (newValue) sizeChecker.stop()
    outdatedStateEventDispatcher.multicaster.eventOccurred()
  }
  private val sizeChecker = ListChangesChecker()

  private var resetDisposable: Disposable

  init {
    requestExecutor.addListener(this) { reset() }
    searchQueryHolder.addQueryChangeListener(this) { reset() }

    Disposer.register(this, sizeChecker)

    resetDisposable = Disposer.newDisposable()
    Disposer.register(this, resetDisposable)
  }

  override val filterNotEmpty: Boolean
    get() = !searchQueryHolder.query.isEmpty()

  override fun resetFilter() {
    searchQueryHolder.query = GiteePullRequestSearchQuery.parseFromString("state:open")
  }

  override fun handleResult(list: List<GiteePullRequest>) {
    listModel.add(list)
    sizeChecker.start()
  }

  override fun reset() {
    super.reset()
    listModel.removeAll()

    outdated = false
    sizeChecker.stop()

    Disposer.dispose(resetDisposable)
    resetDisposable = Disposer.newDisposable()
    Disposer.register(this, resetDisposable)

    loadMore()
  }

  override fun reloadData(request: CompletableFuture<out GiteePullRequest>) {
    request.handleOnEdt(resetDisposable) { result, error ->
      if (error == null && result != null) updateData(result)
    }
  }

  private fun updateData(pullRequest: GiteePullRequest) {
    val index = listModel.items.indexOfFirst { it.id == pullRequest.id }
    listModel.setElementAt(pullRequest, index)
  }

  override fun addOutdatedStateChangeListener(disposable: Disposable, listener: () -> Unit) =
    SimpleEventListener.addDisposableListener(outdatedStateEventDispatcher, disposable, listener)

  private inner class ListChangesChecker : Disposable {

    private var scheduler: ScheduledFuture<*>? = null
    private var progressIndicator: ProgressIndicator? = null

    @Volatile
    private var lastETag: String? = null
      set(value) {
        if (field != null && value != null && field != value) runInEdt { outdated = true }
        field = value
      }

    @CalledInAwt
    fun start() {
      if (scheduler == null) {
        val indicator = NonReusableEmptyProgressIndicator()
        progressIndicator = indicator
        scheduler = JobScheduler.getScheduler().scheduleWithFixedDelay({
          try {
            lastETag = loadListETag(indicator)
          } catch (e: Exception) {
            //ignore
          }
        }, 30, 30, TimeUnit.SECONDS)
      }
    }

    private fun loadListETag(indicator: ProgressIndicator): String? =
      progressManager.runProcess(Computable {
        requestExecutor.execute(GiteeApiRequests.Repos.PullRequests.getListETag(serverPath, repoPath))
      }, indicator)

    @CalledInAwt
    fun stop() {
      scheduler?.cancel(true)
      scheduler = null
      progressIndicator?.cancel()
      progressIndicator = NonReusableEmptyProgressIndicator()
      lastETag = null
    }

    override fun dispose() {
      scheduler?.cancel(true)
      progressIndicator?.cancel()
    }
  }

  companion object {
    private fun buildQuery(searchQuery: GiteePullRequestSearchQuery?, pagination: GiteeRequestPagination? = null): String {
      return GiteeApiUrlQueryBuilder.urlQuery {
        searchQuery?.buildApiSearchQuery(this)
        param(pagination)
      }
    }
  }
}