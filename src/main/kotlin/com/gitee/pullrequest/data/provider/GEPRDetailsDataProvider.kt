// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data.provider

import com.gitee.api.data.GiteeIssueLabel
import com.gitee.api.data.GiteePullRequestDetailed
import com.gitee.api.data.GiteeUser
import com.gitee.api.data.pullrequest.GEPullRequestRequestedReviewer
import com.gitee.util.CollectionDelta
import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.concurrency.annotations.RequiresEdt
import java.util.concurrent.CompletableFuture

interface GEPRDetailsDataProvider {

  @get:RequiresEdt
  val loadedDetails: GiteePullRequestDetailed?

  @RequiresEdt
  fun loadDetails(): CompletableFuture<GiteePullRequestDetailed>

  @RequiresEdt
  fun reloadDetails()

  @RequiresEdt
  fun addDetailsReloadListener(disposable: Disposable, listener: () -> Unit)

  @RequiresEdt
  fun loadDetails(disposable: Disposable, consumer: (CompletableFuture<GiteePullRequestDetailed>) -> Unit) {
    addDetailsReloadListener(disposable) {
      consumer(loadDetails())
    }
    consumer(loadDetails())
  }

  @RequiresEdt
  fun addDetailsLoadedListener(disposable: Disposable, listener: () -> Unit)

  @RequiresEdt
  fun updateDetails(indicator: ProgressIndicator, title: String? = null, description: String? = null): CompletableFuture<GiteePullRequestDetailed>

  @RequiresEdt
  fun adjustReviewers(indicator: ProgressIndicator, delta: CollectionDelta<GEPullRequestRequestedReviewer>)
    : CompletableFuture<Unit>

  @RequiresEdt
  fun adjustAssignees(indicator: ProgressIndicator, delta: CollectionDelta<GiteeUser>)
    : CompletableFuture<Unit>

  @RequiresEdt
  fun adjustLabels(indicator: ProgressIndicator, delta: CollectionDelta<GiteeIssueLabel>)
    : CompletableFuture<Unit>
}