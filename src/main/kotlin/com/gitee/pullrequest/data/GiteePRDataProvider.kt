// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data

import com.gitee.api.data.GiteeCommit
import com.gitee.api.data.GiteePullRequestDetailed
import com.gitee.api.data.pullrequest.GEPullRequestReviewThread
import com.gitee.pullrequest.data.model.GiteePRDiffReviewThreadMapping
import com.intellij.openapi.Disposable
import com.intellij.openapi.vcs.changes.Change
import git4idea.GitCommit
import org.jetbrains.annotations.CalledInAwt
import java.util.*
import java.util.concurrent.CompletableFuture

interface GiteePRDataProvider {
  val number: Long

  val detailsRequest: CompletableFuture<GiteePullRequestDetailed>
  val branchFetchRequest: CompletableFuture<Unit>
  val apiCommitsRequest: CompletableFuture<List<GiteeCommit>>
  val logCommitsRequest: CompletableFuture<List<GitCommit>>
  val reviewThreadsRequest: CompletableFuture<List<GEPullRequestReviewThread>>
  val filesReviewThreadsRequest: CompletableFuture<Map<Change, List<GiteePRDiffReviewThreadMapping>>>

  fun addRequestsChangesListener(listener: RequestsChangedListener)
  fun addRequestsChangesListener(disposable: Disposable, listener: RequestsChangedListener)
  fun removeRequestsChangesListener(listener: RequestsChangedListener)

  @CalledInAwt
  fun reloadDetails()

  @CalledInAwt
  fun reloadCommits()

  @CalledInAwt
  fun reloadComments()

  interface RequestsChangedListener : EventListener {
    fun detailsRequestChanged() {}
    fun commitsRequestChanged() {}
    fun reviewThreadsRequestChanged() {}

  }
}