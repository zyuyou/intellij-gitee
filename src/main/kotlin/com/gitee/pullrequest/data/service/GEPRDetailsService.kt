// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data.service

import com.gitee.api.data.GiteeIssueLabel
import com.gitee.api.data.GiteePullRequestDetailed
import com.gitee.api.data.GiteeUser
import com.gitee.api.data.pullrequest.GEPullRequestRequestedReviewer
import com.gitee.pullrequest.data.GEPRIdentifier
import com.gitee.util.CollectionDelta
import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.annotations.CalledInAny
import java.util.concurrent.CompletableFuture

interface GEPRDetailsService {

  @CalledInAny
  fun loadDetails(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier): CompletableFuture<GiteePullRequestDetailed>

  @CalledInAny
  fun updateDetails(indicator: ProgressIndicator, pullRequestId: GEPRIdentifier, title: String?, description: String?)
    : CompletableFuture<GiteePullRequestDetailed>

  @CalledInAny
  fun adjustReviewers(indicator: ProgressIndicator, pullRequestId: GEPRIdentifier, delta: CollectionDelta<GEPullRequestRequestedReviewer>)
    : CompletableFuture<Unit>

  @CalledInAny
  fun adjustAssignees(indicator: ProgressIndicator, pullRequestId: GEPRIdentifier, delta: CollectionDelta<GiteeUser>)
    : CompletableFuture<Unit>

  @CalledInAny
  fun adjustLabels(indicator: ProgressIndicator, pullRequestId: GEPRIdentifier, delta: CollectionDelta<GiteeIssueLabel>)
    : CompletableFuture<Unit>
}