// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data

import com.gitee.api.data.GEIssueComment
import com.gitee.api.data.GiteePullRequestDetailed
import com.gitee.api.data.pullrequest.GEPullRequestReview
import com.gitee.api.data.pullrequest.timeline.GEPRTimelineItem
import com.gitee.pullrequest.GEPRDiffRequestModelImpl
import com.gitee.pullrequest.data.provider.*
import com.gitee.pullrequest.data.service.*
import com.gitee.util.DisposalCountingHolder
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.util.EventDispatcher
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.messages.ListenerDescriptor
import com.intellij.util.messages.MessageBusFactory
import com.intellij.util.messages.MessageBusOwner
import java.util.*

internal class GEPRDataProviderRepositoryImpl(private val detailsService: GEPRDetailsService,
                                              private val stateService: GEPRStateService,
                                              private val reviewService: GEPRReviewService,
                                              private val filesService: GEPRFilesService,
                                              private val commentService: GEPRCommentService,
                                              private val changesService: GEPRChangesService,
                                              private val timelineLoaderFactory: (GEPRIdentifier) -> GEListLoader<GEPRTimelineItem>)
  : GEPRDataProviderRepository {

  private var isDisposed = false

  private val cache = mutableMapOf<GEPRIdentifier, DisposalCountingHolder<GEPRDataProvider>>()
  private val providerDetailsLoadedEventDispatcher = EventDispatcher.create(DetailsLoadedListener::class.java)

  @RequiresEdt
  override fun getDataProvider(id: GEPRIdentifier, disposable: Disposable): GEPRDataProvider {
    if (isDisposed) throw IllegalStateException("Already disposed")

    return cache.getOrPut(id) {
      DisposalCountingHolder {
        createDataProvider(it, id)
      }.also {
        Disposer.register(it, Disposable { cache.remove(id) })
      }
    }.acquireValue(disposable)
  }

  @RequiresEdt
  override fun findDataProvider(id: GEPRIdentifier): GEPRDataProvider? = cache[id]?.value

  override fun dispose() {
    isDisposed = true
    cache.values.toList().forEach(Disposer::dispose)
  }

  private fun createDataProvider(parentDisposable: Disposable, id: GEPRIdentifier): GEPRDataProvider {
    val messageBus = MessageBusFactory.newMessageBus(object : MessageBusOwner {
      override fun isDisposed() = Disposer.isDisposed(parentDisposable)

      override fun createListener(descriptor: ListenerDescriptor) =
        throw UnsupportedOperationException()
    })
    Disposer.register(parentDisposable, messageBus)

    val detailsData = GEPRDetailsDataProviderImpl(detailsService, id, messageBus).apply {
      addDetailsLoadedListener(parentDisposable) {
        loadedDetails?.let { providerDetailsLoadedEventDispatcher.multicaster.onDetailsLoaded(it) }
      }
    }.also {
      Disposer.register(parentDisposable, it)
    }

    val stateData = GEPRStateDataProviderImpl(stateService, id, messageBus, detailsData).also {
      Disposer.register(parentDisposable, it)
    }
    val changesData = GEPRChangesDataProviderImpl(changesService, id, detailsData).also {
      Disposer.register(parentDisposable, it)
    }
    val reviewData = GEPRReviewDataProviderImpl(reviewService, id, messageBus).also {
      Disposer.register(parentDisposable, it)
    }
    val viewedStateData = GEPRViewedStateDataProviderImpl(filesService, id).also {
      Disposer.register(parentDisposable, it)
    }
    val commentsData = GEPRCommentsDataProviderImpl(commentService, id, messageBus)

    val timelineLoaderHolder = DisposalCountingHolder { timelineDisposable ->
      timelineLoaderFactory(id).also { loader ->
        messageBus.connect(timelineDisposable).subscribe(GEPRDataOperationsListener.TOPIC, object : GEPRDataOperationsListener {
          override fun onStateChanged() = loader.loadMore(true)
          override fun onMetadataChanged() = loader.loadMore(true)

          override fun onCommentAdded() = loader.loadMore(true)
          override fun onCommentUpdated(commentId: String, newBody: String) {
            val comment = loader.loadedData.find { it is GEIssueComment && it.id == commentId } as? GEIssueComment
            if (comment != null) {
              val newComment = GEIssueComment(commentId, comment.author, newBody, comment.createdAt,
                                              comment.viewerCanDelete, comment.viewerCanUpdate)
              loader.updateData(newComment)
            }
            loader.loadMore(true)
          }

          override fun onCommentDeleted(commentId: String) {
            loader.removeData { it is GEIssueComment && it.id == commentId }
            loader.loadMore(true)
          }

          override fun onReviewsChanged() = loader.loadMore(true)

          override fun onReviewUpdated(reviewId: String, newBody: String) {
            val review = loader.loadedData.find { it is GEPullRequestReview && it.id == reviewId } as? GEPullRequestReview
            if (review != null) {
              val newReview = GEPullRequestReview(reviewId, review.url, review.author, newBody, review.state, review.createdAt,
                                                  review.viewerCanUpdate)
              loader.updateData(newReview)
            }
            loader.loadMore(true)
          }
        })
        Disposer.register(timelineDisposable, loader)
      }
    }.also {
      Disposer.register(parentDisposable, it)
    }

    messageBus.connect(stateData).subscribe(GEPRDataOperationsListener.TOPIC, object : GEPRDataOperationsListener {
      override fun onReviewsChanged() = stateData.reloadMergeabilityState()
    })

    return GEPRDataProviderImpl(
      id, detailsData, stateData, changesData, commentsData, reviewData, viewedStateData, timelineLoaderHolder, GEPRDiffRequestModelImpl()
    )
  }

  override fun addDetailsLoadedListener(disposable: Disposable, listener: (GiteePullRequestDetailed) -> Unit) {
    providerDetailsLoadedEventDispatcher.addListener(object : DetailsLoadedListener {
      override fun onDetailsLoaded(details: GiteePullRequestDetailed) {
        listener(details)
      }
    }, disposable)
  }

  private interface DetailsLoadedListener : EventListener {
    fun onDetailsLoaded(details: GiteePullRequestDetailed)
  }
}