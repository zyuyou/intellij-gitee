// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data.provider

import com.gitee.api.data.GiteeIssueLabel
import com.gitee.api.data.GiteePullRequestDetailed
import com.gitee.api.data.GiteeUser
import com.gitee.api.data.pullrequest.GEPullRequestRequestedReviewer
import com.gitee.pullrequest.data.GEPRIdentifier
import com.gitee.pullrequest.data.service.GEPRDetailsService
import com.gitee.util.CollectionDelta
import com.gitee.util.LazyCancellableBackgroundProcessValue
import com.intellij.collaboration.async.CompletableFutureUtil.completionOnEdt
import com.intellij.collaboration.async.CompletableFutureUtil.successOnEdt
import com.intellij.collaboration.ui.SimpleEventListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.EventDispatcher
import com.intellij.util.messages.MessageBus
import java.util.concurrent.CompletableFuture
import kotlin.properties.Delegates

class GEPRDetailsDataProviderImpl(private val detailsService: GEPRDetailsService,
                                  private val pullRequestId: GEPRIdentifier,
                                  private val messageBus: MessageBus)
  : GEPRDetailsDataProvider, Disposable {

  private val detailsLoadedEventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)

  override var loadedDetails by Delegates.observable<GiteePullRequestDetailed?>(null) { _, _, _ ->
    detailsLoadedEventDispatcher.multicaster.eventOccurred()
  }
    private set

  private val detailsRequestValue = LazyCancellableBackgroundProcessValue.create { indicator ->
    detailsService.loadDetails(indicator, pullRequestId).successOnEdt {
      loadedDetails = it
      it
    }
  }

  override fun loadDetails(): CompletableFuture<GiteePullRequestDetailed> = detailsRequestValue.value

  override fun reloadDetails() = detailsRequestValue.drop()

  override fun updateDetails(indicator: ProgressIndicator, title: String?, description: String?): CompletableFuture<GiteePullRequestDetailed> {
    val future = detailsService.updateDetails(indicator, pullRequestId, title, description).completionOnEdt {
      messageBus.syncPublisher(GEPRDataOperationsListener.TOPIC).onMetadataChanged()
    }
    detailsRequestValue.overrideProcess(future.successOnEdt {
      loadedDetails = it
      it
    })
    return future
  }

  override fun adjustReviewers(indicator: ProgressIndicator,
                               delta: CollectionDelta<GEPullRequestRequestedReviewer>
  ): CompletableFuture<Unit> {
    return detailsService.adjustReviewers(indicator, pullRequestId, delta).notify()
  }

  override fun adjustAssignees(indicator: ProgressIndicator, delta: CollectionDelta<GiteeUser>): CompletableFuture<Unit> {
    return detailsService.adjustAssignees(indicator, pullRequestId, delta).notify()
  }

  override fun adjustLabels(indicator: ProgressIndicator, delta: CollectionDelta<GiteeIssueLabel>): CompletableFuture<Unit> {
    return detailsService.adjustLabels(indicator, pullRequestId, delta).notify()
  }

  override fun addDetailsReloadListener(disposable: Disposable, listener: () -> Unit) =
    detailsRequestValue.addDropEventListener(disposable, listener)

  override fun addDetailsLoadedListener(disposable: Disposable, listener: () -> Unit) =
    SimpleEventListener.addDisposableListener(detailsLoadedEventDispatcher, disposable, listener)

  private fun <T> CompletableFuture<T>.notify(): CompletableFuture<T> =
    completionOnEdt {
      detailsRequestValue.drop()
      messageBus.syncPublisher(GEPRDataOperationsListener.TOPIC).onMetadataChanged()
    }

  override fun dispose() {
    detailsRequestValue.drop()
  }
}