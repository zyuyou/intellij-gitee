// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.comment.ui

import com.gitee.api.data.pullrequest.GEPullRequestPendingReview
import com.intellij.collaboration.ui.SimpleEventListener
import com.intellij.util.EventDispatcher

class GEPRReviewProcessModelImpl : GEPRReviewProcessModel {

  private val changeEventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)

  override var pendingReview: GEPullRequestPendingReview? = null
    private set
  override var isActual: Boolean = false
    private set

  override fun populatePendingReviewData(review: GEPullRequestPendingReview?) {
    pendingReview = review
    isActual = true
    changeEventDispatcher.multicaster.eventOccurred()
  }

  override fun clearPendingReviewData() {
    pendingReview = null
    isActual = false
    changeEventDispatcher.multicaster.eventOccurred()
  }

  override fun addAndInvokeChangesListener(listener: SimpleEventListener) {
    changeEventDispatcher.addListener(listener)
    listener.eventOccurred()
  }

  override fun removeChangesListener(listener: SimpleEventListener) {
    changeEventDispatcher.removeListener(listener)
  }
}
