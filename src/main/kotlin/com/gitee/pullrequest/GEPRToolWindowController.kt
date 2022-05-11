// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.gitee.pullrequest

import com.gitee.pullrequest.ui.toolwindow.GEPRToolWindowTabController
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.concurrency.annotations.RequiresEdt

@Service
internal class GEPRToolWindowController(private val project: Project) : Disposable {
  @RequiresEdt
  fun isAvailable(): Boolean {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(GEPRToolWindowFactory.ID) ?: return false
    return toolWindow.isAvailable
  }

  @RequiresEdt
  fun activate(onActivated: ((GEPRToolWindowTabController) -> Unit)? = null) {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(GEPRToolWindowFactory.ID) ?: return
    toolWindow.activate {
      val controller = toolWindow.contentManager.selectedContent?.getUserData(GEPRToolWindowTabController.KEY)
      if (controller != null && onActivated != null) {
        onActivated(controller)
      }
    }
  }

  fun getTabController(): GEPRToolWindowTabController? {
    return ToolWindowManager.getInstance(project)
      .getToolWindow(GEPRToolWindowFactory.ID)
      ?.let { it.contentManagerIfCreated?.selectedContent?.getUserData(GEPRToolWindowTabController.KEY) }
  }

  override fun dispose() {
  }
}