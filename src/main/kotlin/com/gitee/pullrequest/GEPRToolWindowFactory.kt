// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.gitee.pullrequest

import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.pullrequest.action.GEPRSelectPullRequestForFileAction
import com.gitee.pullrequest.action.GEPRSwitchRemoteAction
import com.gitee.pullrequest.config.GiteePullRequestsProjectUISettings
import com.gitee.pullrequest.data.GEPRDataContextRepository
import com.gitee.pullrequest.ui.toolwindow.GEPRToolWindowTabController
import com.gitee.pullrequest.ui.toolwindow.GEPRToolWindowTabControllerImpl
import com.gitee.util.GEGitRepositoryMapping
import com.gitee.util.GEProjectRepositoriesManager
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.EmptyAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.openapi.wm.ex.ToolWindowEx
import com.intellij.openapi.wm.impl.content.ToolWindowContentUi
import javax.swing.JPanel

internal class GEPRToolWindowFactory : ToolWindowFactory, DumbAware {
  override fun init(toolWindow: ToolWindow) {
    ApplicationManager.getApplication().messageBus.connect(toolWindow.disposable)
      .subscribe(GEProjectRepositoriesManager.LIST_CHANGES_TOPIC, object : GEProjectRepositoriesManager.ListChangeListener {
        override fun repositoryListChanged(newList: Set<GEGitRepositoryMapping>, project: Project) {
          toolWindow.isAvailable = newList.isNotEmpty()
        }
      })
  }

  override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) = with(toolWindow as ToolWindowEx) {
    setTitleActions(
      listOf(EmptyAction.registerWithShortcutSet("Gitee.Create.Pull.Request", CommonShortcuts.getNew(), component), GEPRSelectPullRequestForFileAction())
    )

    setAdditionalGearActions(DefaultActionGroup(GEPRSwitchRemoteAction()))
    component.putClientProperty(ToolWindowContentUi.HIDE_ID_LABEL, "true")

    with(contentManager) {
      addContent(
        factory.createContent(JPanel(null), null, false).apply {
          isCloseable = false
          setDisposer(Disposer.newDisposable("GEPR tab disposable"))
        }.also {
          val authManager = GiteeAuthenticationManager.getInstance()
          val repositoryManager = project.service<GEProjectRepositoriesManager>()
          val dataContextRepository = GEPRDataContextRepository.getInstance(project)
          val projectString = GiteePullRequestsProjectUISettings.getInstance(project)

          it.putUserData(
            GEPRToolWindowTabController.KEY,
            GEPRToolWindowTabControllerImpl(project, authManager, repositoryManager, dataContextRepository, projectString, it)
          )
        })
    }
  }

  override fun shouldBeAvailable(project: Project): Boolean = false

  companion object {
    const val ID = "Gitee Pull Requests"
  }
}