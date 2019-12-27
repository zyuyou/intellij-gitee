// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data.service

import com.gitee.api.data.GiteeIssueLabel
import com.gitee.api.data.GiteeUser
import com.gitee.util.CollectionDelta
import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.annotations.CalledInAwt
import org.jetbrains.annotations.CalledInBackground
import java.util.concurrent.CompletableFuture

interface GiteePullRequestsMetadataService : Disposable {
  val collaboratorsWithPushAccess: CompletableFuture<List<GiteeUser>>
  val issuesAssignees: CompletableFuture<List<GiteeUser>>
  val labels: CompletableFuture<List<GiteeIssueLabel>>

  @CalledInAwt
  fun resetData()

  @CalledInBackground
  fun adjustReviewers(indicator: ProgressIndicator, pullRequest: Long, delta: CollectionDelta<GiteeUser>)

  @CalledInBackground
  fun adjustAssignees(indicator: ProgressIndicator, pullRequest: Long, delta: CollectionDelta<GiteeUser>)

  @CalledInBackground
  fun adjustLabels(indicator: ProgressIndicator, pullRequest: Long, delta: CollectionDelta<GiteeIssueLabel>)
}