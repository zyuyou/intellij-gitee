// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.action

import com.gitee.i18n.GiteeBundle
import com.gitee.pullrequest.GEPRToolWindowController
import com.gitee.pullrequest.GEPRVirtualFile
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAwareAction
import java.util.function.Supplier

class GEPRSelectPullRequestForFileAction : DumbAwareAction(GiteeBundle.messagePointer("pull.request.select.action"),
                                                           Supplier<String?> { null },
                                                           AllIcons.General.Locate) {

  override fun update(e: AnActionEvent) {
    val project = e.project
    if (project == null) {
      e.presentation.isEnabledAndVisible = false
      return
    }

    val componentController = project.service<GEPRToolWindowController>().getTabController()?.componentController
    if (componentController == null) {
      e.presentation.isEnabledAndVisible = false
      return
    }

    e.presentation.isVisible = true
    val files = FileEditorManager.getInstance(project).selectedFiles.filterIsInstance<GEPRVirtualFile>()
    e.presentation.isEnabled = files.isNotEmpty()
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getRequiredData(PlatformDataKeys.PROJECT)
    val file = FileEditorManager.getInstance(project).selectedFiles.filterIsInstance<GEPRVirtualFile>().first()
    project.service<GEPRToolWindowController>().activate {
      it.componentController?.viewPullRequest(file.pullRequest)
    }
  }
}