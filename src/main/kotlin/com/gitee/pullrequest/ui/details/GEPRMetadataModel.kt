// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.details

import com.gitee.api.data.GiteeIssueLabel
import com.gitee.api.data.GiteeUser
import com.gitee.api.data.pullrequest.GEPullRequestRequestedReviewer
import com.gitee.util.CollectionDelta
import com.intellij.openapi.progress.ProgressIndicator
import java.util.concurrent.CompletableFuture

interface GEPRMetadataModel {
  val assignees: List<GiteeUser>
  val reviewers: List<GEPullRequestRequestedReviewer>
  val labels: List<GiteeIssueLabel>

  val isEditingAllowed: Boolean

  fun loadPotentialReviewers(): CompletableFuture<List<GEPullRequestRequestedReviewer>>
  fun adjustReviewers(indicator: ProgressIndicator, delta: CollectionDelta<GEPullRequestRequestedReviewer>): CompletableFuture<Unit>

  fun loadPotentialAssignees(): CompletableFuture<List<GiteeUser>>
  fun adjustAssignees(indicator: ProgressIndicator, delta: CollectionDelta<GiteeUser>): CompletableFuture<Unit>

  fun loadAssignableLabels(): CompletableFuture<List<GiteeIssueLabel>>
  fun adjustLabels(indicator: ProgressIndicator, delta: CollectionDelta<GiteeIssueLabel>): CompletableFuture<Unit>

  fun addAndInvokeChangesListener(listener: () -> Unit)
}