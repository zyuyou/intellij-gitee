// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data.provider

import com.gitee.api.data.GEPullRequestReviewEvent
import com.gitee.api.data.pullrequest.GEPullRequestPendingReview
import com.gitee.api.data.pullrequest.GEPullRequestReviewComment
import com.gitee.api.data.pullrequest.GEPullRequestReviewThread
import com.gitee.api.data.request.GEPullRequestDraftReviewComment
import com.gitee.api.data.request.GEPullRequestDraftReviewThread
import com.intellij.diff.util.Side
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Document
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.concurrency.annotations.RequiresEdt
import java.util.concurrent.CompletableFuture

interface GEPRReviewDataProvider {

  val submitReviewCommentDocument: Document

  @RequiresEdt
  fun loadPendingReview(): CompletableFuture<GEPullRequestPendingReview?>

  @RequiresEdt
  fun resetPendingReview()

  @RequiresEdt
  fun loadReviewThreads(): CompletableFuture<List<GEPullRequestReviewThread>>

  @RequiresEdt
  fun resetReviewThreads()

  @RequiresEdt
  fun submitReview(progressIndicator: ProgressIndicator, reviewId: String, event: GEPullRequestReviewEvent, body: String? = null)
    : CompletableFuture<out Any?>

  @RequiresEdt
  fun createReview(progressIndicator: ProgressIndicator,
                   event: GEPullRequestReviewEvent? = null, body: String? = null,
                   commitSha: String? = null,
                   comments: List<GEPullRequestDraftReviewComment>? = null,
                   threads: List<GEPullRequestDraftReviewThread>? = null)
    : CompletableFuture<GEPullRequestPendingReview>

  @RequiresEdt
  fun updateReviewBody(progressIndicator: ProgressIndicator, reviewId: String, newText: String): CompletableFuture<String>

  @RequiresEdt
  fun deleteReview(progressIndicator: ProgressIndicator, reviewId: String): CompletableFuture<out Any?>

  @RequiresEdt
  fun canComment(): Boolean

  @RequiresEdt
  fun addComment(progressIndicator: ProgressIndicator, reviewId: String, body: String, commitSha: String, fileName: String, diffLine: Int)
    : CompletableFuture<out GEPullRequestReviewComment>

  @RequiresEdt
  fun addComment(progressIndicator: ProgressIndicator, replyToCommentId: String, body: String)
    : CompletableFuture<out GEPullRequestReviewComment>

  @RequiresEdt
  fun deleteComment(progressIndicator: ProgressIndicator, commentId: String)
    : CompletableFuture<out Any>

  @RequiresEdt
  fun updateComment(progressIndicator: ProgressIndicator, commentId: String, newText: String)
    : CompletableFuture<GEPullRequestReviewComment>

  @RequiresEdt
  fun createThread(progressIndicator: ProgressIndicator, reviewId: String?, body: String, line: Int, side: Side, startLine: Int, fileName: String)
    : CompletableFuture<GEPullRequestReviewThread>

  @RequiresEdt
  fun resolveThread(progressIndicator: ProgressIndicator, id: String): CompletableFuture<GEPullRequestReviewThread>

  @RequiresEdt
  fun unresolveThread(progressIndicator: ProgressIndicator, id: String): CompletableFuture<GEPullRequestReviewThread>

  @RequiresEdt
  fun addReviewThreadsListener(disposable: Disposable, listener: () -> Unit)

  @RequiresEdt
  fun addPendingReviewListener(disposable: Disposable, listener: () -> Unit)
}