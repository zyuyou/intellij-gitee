// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.action

import com.gitee.i18n.GiteeBundle
import com.gitee.pullrequest.GEPRToolWindowController
import com.gitee.pullrequest.ui.toolwindow.GEPRToolWindowTabController
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction

class GEPRSwitchRemoteAction : DumbAwareAction(GiteeBundle.message("pull.request.change.remote.or.account")) {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = isEnabledAndVisible(e)
  }

  private fun isEnabledAndVisible(e: AnActionEvent): Boolean {
    val controller = e.project?.service<GEPRToolWindowController>()?.getTabController() ?: return false
    return controller.canResetRemoteOrAccount()
  }

  override fun actionPerformed(e: AnActionEvent) = e.project!!.service<GEPRToolWindowController>()
    .activate(GEPRToolWindowTabController::resetRemoteAndAccount)
}