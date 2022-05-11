// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.details.action

import com.gitee.pullrequest.ui.details.GEPRStateModel
import com.intellij.openapi.util.NlsActions

abstract class GEPRMergeAction(@NlsActions.ActionText actionName: String, stateModel: GEPRStateModel)
  : GEPRStateChangeAction(actionName, stateModel) {

  init {
    stateModel.addAndInvokeMergeabilityStateLoadingResultListener(::update)
  }

  override fun computeEnabled(): Boolean {
    val mergeability = stateModel.mergeabilityState
    return super.computeEnabled() && mergeability != null && mergeability.canBeMerged
  }
}