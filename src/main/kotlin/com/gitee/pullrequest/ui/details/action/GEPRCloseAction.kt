// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.details.action

import com.gitee.i18n.GiteeBundle
import com.gitee.pullrequest.ui.details.GEPRStateModel
import java.awt.event.ActionEvent

internal class GEPRCloseAction(stateModel: GEPRStateModel)
  : GEPRStateChangeAction(GiteeBundle.message("pull.request.close.action"), stateModel) {

  override fun actionPerformed(e: ActionEvent?) = stateModel.submitCloseTask()
}