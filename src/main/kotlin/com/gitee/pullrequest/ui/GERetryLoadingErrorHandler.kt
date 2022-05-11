// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui

import com.gitee.i18n.GiteeBundle
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action

open class GERetryLoadingErrorHandler(protected val resetRunnable: () -> Unit) : GELoadingErrorHandler {
  override fun getActionForError(error: Throwable): Action? = RetryAction()

  protected inner class RetryAction : AbstractAction(GiteeBundle.message("retry.action")) {
    override fun actionPerformed(e: ActionEvent?) {
      resetRunnable()
    }
  }
}