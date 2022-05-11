// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.actions

import com.gitee.i18n.GiteeBundle
import com.gitee.icons.GiteeIcons
import com.gitee.pullrequest.GEPRToolWindowController
import com.gitee.pullrequest.ui.toolwindow.GEPRToolWindowViewType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import java.util.function.Supplier

class GiteeViewPullRequestsAction :
  DumbAwareAction(GiteeBundle.messagePointer("pull.request.view.list"),
                  Supplier { null },
                  GiteeIcons.Gitee_icon) {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = isEnabledAndVisible(e)
  }

  private fun isEnabledAndVisible(e: AnActionEvent): Boolean {
    val project = e.project ?: return false
    return project.service<GEPRToolWindowController>().isAvailable()
  }

  override fun actionPerformed(e: AnActionEvent) {
    e.project!!.service<GEPRToolWindowController>().activate {
      it.initialView = GEPRToolWindowViewType.LIST
      it.componentController?.viewList()
    }
  }
}