// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.ui

import com.gitee.pullrequest.GiteePRAccountsComponent
import com.gitee.pullrequest.GiteePRComponentFactory
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentManager
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.openapi.wm.impl.ToolWindowImpl
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentManagerListener
import com.intellij.vcs.log.impl.VcsLogEditorTabSelector

class GiteePREditorContentSynchronizer {
  companion object {
    fun getInstance(project: Project): GiteePREditorContentSynchronizer = project.service()
  }

  private fun addContentManagerListener(window: ToolWindowImpl,
                                        listener: ContentManagerListener) {
    window.contentManager.addContentManagerListener(listener)
    Disposer.register(window.contentManager, Disposable {
      if (!window.isDisposed) {
        window.contentManager.removeContentManagerListener(listener)
      }
    })
  }

  class MyToolwindowListener(private val project: Project) : ToolWindowManagerListener {
    override fun toolWindowRegistered(id: String) {

      if (!Registry.`is`("show.log.as.editor.tab")) return
      if (id != ChangesViewContentManager.TOOLWINDOW_ID) return

      val toolwindow = ToolWindowManager.getInstance(project).getToolWindow(id) as? ToolWindowImpl
      if (toolwindow != null) {
        getInstance(project).addContentManagerListener(toolwindow, MyLogEditorListener(project))
      }
    }
  }

  private class MyLogEditorListener(private val project: Project) : VcsLogEditorTabSelector(project) {
    override fun selectEditorTab(content: Content) {
      val component = content.component
      val ghprAccountsComponent = component as? GiteePRAccountsComponent
      if (ghprAccountsComponent != null) {
        project.service<GiteePRComponentFactory>().tryOpenGiteePREditorTab()
      }
    }
  }

}