// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data.service

import com.gitee.api.data.GEPullRequestReviewEvent
import com.gitee.api.data.pullrequest.*
import com.gitee.api.data.request.GEPullRequestDraftReviewComment
import com.gitee.api.data.request.GEPullRequestDraftReviewThread
import com.gitee.pullrequest.data.GEPRIdentifier
import com.intellij.diff.util.Side
import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.annotations.CalledInAny
import java.util.concurrent.CompletableFuture

interface GEPRReviewService {

  fun canComment(): Boolean

  @CalledInAny
  fun loadPendingReview(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier)
    : CompletableFuture<GEPullRequestPendingReview?>

  @CalledInAny
  fun loadReviewThreads(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier)
    : CompletableFuture<List<GEPullRequestReviewThread>>

  @CalledInAny
  fun createReview(progressIndicator: ProgressIndicator,
                   pullRequestId: GEPRIdentifier,
                   event: GEPullRequestReviewEvent? = null,
                   body: String? = null,
                   commitSha: String? = null,
                   comments: List<GEPullRequestDraftReviewComment>? = null,
                   threads: List<GEPullRequestDraftReviewThread>? = null): CompletableFuture<GEPullRequestPendingReview>

  @CalledInAny
  fun submitReview(progressIndicator: ProgressIndicator,
                   pullRequestId: GEPRIdentifier,
                   reviewId: String,
                   event: GEPullRequestReviewEvent,
                   body: String?): CompletableFuture<out Any?>

  @CalledInAny
  fun updateReviewBody(progressIndicator: ProgressIndicator, reviewId: String, newText: String): CompletableFuture<GEPullRequestReview>

  @CalledInAny
  fun deleteReview(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier, reviewId: String): CompletableFuture<out Any?>

  @CalledInAny
  fun addComment(progressIndicator: ProgressIndicator,
                 pullRequestId: GEPRIdentifier,
                 reviewId: String,
                 replyToCommentId: String,
                 body: String)
    : CompletableFuture<GEPullRequestReviewCommentWithPendingReview>

  @CalledInAny
  fun addComment(progressIndicator: ProgressIndicator, reviewId: String,
                 body: String, commitSha: String, fileName: String, diffLine: Int)
    : CompletableFuture<GEPullRequestReviewCommentWithPendingReview>

  @CalledInAny
  fun deleteComment(progressIndicator: ProgressIndicator,
                    pullRequestId: GEPRIdentifier,
                    commentId: String): CompletableFuture<GEPullRequestPendingReview>

  @CalledInAny
  fun updateComment(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier, commentId: String, newText: String)
    : CompletableFuture<GEPullRequestReviewComment>

  @CalledInAny
  fun addThread(progressIndicator: ProgressIndicator, reviewId: String,
                body: String, line: Int, side: Side, startLine: Int, fileName: String)
    : CompletableFuture<GEPullRequestReviewThread>

  @CalledInAny
  fun resolveThread(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier, id: String)
    : CompletableFuture<GEPullRequestReviewThread>

  @CalledInAny
  fun unresolveThread(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier, id: String)
    : CompletableFuture<GEPullRequestReviewThread>
}
