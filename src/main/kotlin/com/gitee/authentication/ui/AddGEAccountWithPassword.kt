// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.ui

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeServerPath
import com.gitee.i18n.GiteeBundle.message
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.ui.components.BrowserLink
import com.intellij.util.ui.JBUI
import git4idea.i18n.GitBundle
import java.awt.Component
import javax.swing.JComponent
import javax.swing.JPanel

class AddGEAccountWithPasswordAction : DumbAwareAction() {
  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = e.getData(GEAccountsHost.KEY) != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    val accountsHost = e.getData(GEAccountsHost.KEY)!!
    val dialog = GEPasswordLoginDialog(e.project, e.getData(PlatformDataKeys.CONTEXT_COMPONENT), accountsHost::isAccountUnique)
    dialog.setServer(GiteeServerPath.DEFAULT_HOST, false)

    if (dialog.showAndGet()) {
      accountsHost.addAccount(dialog.server, dialog.login, dialog.credentials)
    }
  }
}

internal class GEPasswordLoginDialog(project: Project?, parent: Component?, isAccountUnique: UniqueLoginPredicate) :
  BaseLoginDialog(project, parent, GiteeApiRequestExecutor.Factory.getInstance(), isAccountUnique) {

  init {
    title = message("login.to.gitee")
    setOKButtonText(GitBundle.message("login.dialog.button.login"))
    init()
  }

  override fun createCenterPanel(): JComponent = loginPanel.setPaddingCompensated()

  override fun createSouthAdditionalPanel(): JPanel {
    return JBUI.Panels.simplePanel().addToCenter(
      BrowserLink(message("login.sign.up"), "https://gitee.com")
    )
  }
}