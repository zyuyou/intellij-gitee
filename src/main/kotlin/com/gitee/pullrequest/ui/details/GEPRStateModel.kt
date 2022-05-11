// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.details

import com.gitee.pullrequest.data.GEPRMergeabilityState

interface GEPRStateModel {

  val viewerDidAuthor: Boolean
  val isDraft: Boolean
  val mergeabilityState: GEPRMergeabilityState?
  val mergeabilityLoadingError: Throwable?

  var isBusy: Boolean
  var actionError: Throwable?

  fun reloadMergeabilityState()

  fun submitCloseTask()
  fun submitReopenTask()
  fun submitMarkReadyForReviewTask()
  fun submitMergeTask()
  fun submitRebaseMergeTask()
  fun submitSquashMergeTask()

  fun addAndInvokeDraftStateListener(listener: () -> Unit)
  fun addAndInvokeMergeabilityStateLoadingResultListener(listener: () -> Unit)
  fun addAndInvokeBusyStateChangedListener(listener: () -> Unit)
  fun addAndInvokeActionErrorChangedListener(listener: () -> Unit)
}
