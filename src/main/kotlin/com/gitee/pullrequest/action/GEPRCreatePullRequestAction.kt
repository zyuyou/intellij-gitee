// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.action

import com.gitee.i18n.GiteeBundle
import com.gitee.icons.GiteeIcons
import com.gitee.pullrequest.GEPRToolWindowController
import com.gitee.pullrequest.ui.toolwindow.GEPRToolWindowViewType
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import java.util.function.Supplier

class GEPRCreatePullRequestAction : DumbAwareAction(GiteeBundle.messagePointer("pull.request.create.show.form.action"),
                                                    Supplier { null },
                                                    AllIcons.General.Add) {

  override fun update(e: AnActionEvent) {
    with(e) {
      val twController = project?.service<GEPRToolWindowController>()
      val twAvailable = project != null && twController != null && twController.isAvailable()
      val componentController = twController?.getTabController()?.componentController
      val twInitialized = project != null && componentController != null

      if (place == ActionPlaces.TOOLWINDOW_TITLE) {
        presentation.isEnabledAndVisible = twInitialized && componentController?.currentView != GEPRToolWindowViewType.NEW
        presentation.icon = AllIcons.General.Add
      }
      else {
        presentation.isEnabledAndVisible = twAvailable
        presentation.icon = GiteeIcons.Gitee_icon
      }
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    val twController = e.getRequiredData(PlatformDataKeys.PROJECT).service<GEPRToolWindowController>()
    twController.activate {
      it.initialView = GEPRToolWindowViewType.NEW
      it.componentController?.createPullRequest()
    }
  }
}