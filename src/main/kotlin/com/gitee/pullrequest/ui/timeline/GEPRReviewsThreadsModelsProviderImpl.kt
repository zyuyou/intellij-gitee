// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.timeline

import com.gitee.api.data.pullrequest.GEPullRequestReviewThread
import com.gitee.pullrequest.data.provider.GEPRReviewDataProvider
import com.intellij.collaboration.async.CompletableFutureUtil.handleOnEdt
import com.intellij.openapi.Disposable

class GEPRReviewsThreadsModelsProviderImpl(private val reviewDataProvider: GEPRReviewDataProvider,
                                           private val parentDisposable: Disposable)
  : GEPRReviewsThreadsModelsProvider {

  private var threadsByReview = mapOf<String, List<GEPullRequestReviewThread>>()
  private var loading = false
  private val threadsModelsByReview = mutableMapOf<String, GEPRReviewThreadsModel>()
  private var threadsUpdateRequired = false

  init {
    reviewDataProvider.addReviewThreadsListener(parentDisposable) {
      if (threadsModelsByReview.isNotEmpty()) requestUpdateReviewsThreads()
    }
  }

  override fun getReviewThreadsModel(reviewId: String): GEPRReviewThreadsModel {
    return threadsModelsByReview.getOrPut(reviewId) {
      GEPRReviewThreadsModel()
    }.apply {
      val loadedThreads = threadsByReview[reviewId]
      threadsUpdateRequired = true
      if (loadedThreads == null && !loading) requestUpdateReviewsThreads()
      else update(loadedThreads.orEmpty())
    }
  }

  private fun updateReviewsThreads(threads: List<GEPullRequestReviewThread>) {
    val threadsMap = mutableMapOf<String, MutableList<GEPullRequestReviewThread>>()
    for (thread in threads) {
      val reviewId = thread.reviewId
      if (reviewId != null) {
        val list = threadsMap.getOrPut(reviewId) { mutableListOf() }
        list.add(thread)
      }
    }
    threadsByReview = threadsMap
    for ((reviewId, model) in threadsModelsByReview) {
      model.update(threadsByReview[reviewId].orEmpty())
    }
  }

  private fun requestUpdateReviewsThreads() {
    loading = true
    threadsUpdateRequired = false
    reviewDataProvider.loadReviewThreads().handleOnEdt(parentDisposable) { threads, _ ->
      if (threads != null) {
        updateReviewsThreads(threads)
        loading = false
        if (threadsUpdateRequired) requestUpdateReviewsThreads()
      }
      else {
        loading = false
      }
    }
  }
}