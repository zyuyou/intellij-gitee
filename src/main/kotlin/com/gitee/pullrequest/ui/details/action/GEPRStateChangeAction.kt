// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.details.action

import com.gitee.pullrequest.ui.details.GEPRStateModel
import com.intellij.openapi.util.NlsActions
import javax.swing.AbstractAction

abstract class GEPRStateChangeAction(@NlsActions.ActionText actionName: String, protected val stateModel: GEPRStateModel)
  : AbstractAction(actionName) {

  init {
    stateModel.addAndInvokeBusyStateChangedListener(::update)
  }

  protected fun update() {
    isEnabled = computeEnabled()
  }

  protected open fun computeEnabled(): Boolean = !stateModel.isBusy
}