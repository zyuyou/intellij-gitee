// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.comment.ui

import com.gitee.api.data.pullrequest.GEPullRequestPendingReview
import com.intellij.collaboration.ui.SimpleEventListener

interface GEPRReviewProcessModel {
  val pendingReview: GEPullRequestPendingReview?
  val isActual: Boolean

  fun populatePendingReviewData(review: GEPullRequestPendingReview?)
  fun clearPendingReviewData()

  fun addAndInvokeChangesListener(listener: SimpleEventListener)
  fun removeChangesListener(listener: SimpleEventListener)
}