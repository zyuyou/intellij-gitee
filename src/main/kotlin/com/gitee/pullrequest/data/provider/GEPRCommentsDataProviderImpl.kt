// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data.provider

import com.gitee.api.data.GiteeIssueCommentWithHtml
import com.gitee.pullrequest.data.GEPRIdentifier
import com.gitee.pullrequest.data.service.GEPRCommentService
import com.intellij.collaboration.async.CompletableFutureUtil.successOnEdt
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.messages.MessageBus
import java.util.concurrent.CompletableFuture

class GEPRCommentsDataProviderImpl(private val commentService: GEPRCommentService,
                                   private val pullRequestId: GEPRIdentifier,
                                   private val messageBus: MessageBus) : GEPRCommentsDataProvider {

  override fun addComment(progressIndicator: ProgressIndicator,
                          body: String): CompletableFuture<GiteeIssueCommentWithHtml> =
    commentService.addComment(progressIndicator, pullRequestId, body).successOnEdt {
      messageBus.syncPublisher(GEPRDataOperationsListener.TOPIC).onCommentAdded()
      it
    }

  override fun updateComment(progressIndicator: ProgressIndicator, commentId: String, text: String): CompletableFuture<String> =
    commentService.updateComment(progressIndicator, commentId, text).successOnEdt {
      messageBus.syncPublisher(GEPRDataOperationsListener.TOPIC).onCommentUpdated(commentId, it.body)
      it
    }.thenApply { it.body }

  override fun deleteComment(progressIndicator: ProgressIndicator, commentId: String): CompletableFuture<out Any?> =
    commentService.deleteComment(progressIndicator, commentId).successOnEdt {
      messageBus.syncPublisher(GEPRDataOperationsListener.TOPIC).onCommentDeleted(commentId)
    }
}