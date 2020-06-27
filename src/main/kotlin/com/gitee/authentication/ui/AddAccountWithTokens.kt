// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.ui

import com.gitee.GiteeBundle.Companion.message
import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeServerPath
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import java.awt.Component
import javax.swing.JComponent

class AddGEAccountWithTokensAction : BaseAddAccountWithTokensAction() {
  override val defaultServer: String get() = GiteeServerPath.DEFAULT_HOST
}

class AddGEEAccountAction : BaseAddAccountWithTokensAction() {
  override val defaultServer: String get() = ""
}

abstract class BaseAddAccountWithTokensAction : DumbAwareAction() {
  abstract val defaultServer: String

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = e.getData(GiteeAccountsPanel.KEY) != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    val accountsPanel = e.getData(GiteeAccountsPanel.KEY)!!
    val dialog = AddAccountWithTokensDialog(e.project, accountsPanel, defaultServer, accountsPanel::isAccountUnique)

    if (dialog.showAndGet()) {
      accountsPanel.addAccount(dialog.server, dialog.login, dialog.accessToken to dialog.refreshToken)
    }
  }
}

private class AddAccountWithTokensDialog(project: Project?, parent: Component?, server: String, isAccountUnique: UniqueLoginPredicate) :
  BaseLoginDialog(project, parent, GiteeApiRequestExecutor.Factory.getInstance(), isAccountUnique) {

  init {
    title = message("dialog.title.add.gitee.account")
    setOKButtonText(message("button.add.account"))

    setServer(server, server != GiteeServerPath.DEFAULT_HOST)
    loginPanel.setTokenUi()

    init()
  }

  override fun createCenterPanel(): JComponent = loginPanel.setPaddingCompensated()
}