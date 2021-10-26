// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.ui

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeServerPath
import com.gitee.authentication.ui.GiteeLoginDialog.Companion.createSignUpLink
import com.gitee.i18n.GiteeBundle.message
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI.Panels.simplePanel
import git4idea.i18n.GitBundle
import java.awt.Component
import javax.swing.Action
import javax.swing.JComponent
import javax.swing.JPanel

class AddGEAccountAction : DumbAwareAction() {
  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = e.getData(GEAccountsHost.KEY) != null
  }

  override fun actionPerformed(e: AnActionEvent) {
//    val accountsPanel = e.getData(GiteeAccountsPanel.KEY)!!
//    val dialog = createDialog(e.project, accountsPanel)
//    dialog.setServer(GiteeServerPath.DEFAULT_HOST, false)
//
//    if (dialog.showAndGet()) {
//      accountsPanel.addAccount(dialog.server, dialog.login, dialog.accessToken to dialog.refreshToken)
//    }

//    val accountsHost = e.getData(GEAccountsHost.KEY)!!
//    val dialog = GEOAuthLoginDialog(e.project, e.getData(PlatformDataKeys.CONTEXT_COMPONENT), accountsHost::isAccountUnique)
//    dialog.setServer(GiteeServerPath.DEFAULT_HOST, false)
//
//    if (dialog.showAndGet()) {
//      accountsHost.addAccount(dialog.server, dialog.login, dialog.token)
//    }

    val accountsHost = e.getData(GEAccountsHost.KEY)!!
    val dialog = GEPasswordLoginDialog(e.project, e.getData(PlatformDataKeys.CONTEXT_COMPONENT), accountsHost::isAccountUnique)
    dialog.setServer(GiteeServerPath.DEFAULT_HOST, false)

    if (dialog.showAndGet()) {
      accountsHost.addAccount(dialog.server, dialog.login, dialog.accessToken to dialog.refreshToken)
    }


  }

//  private fun createDialog(project: Project?, accountsPanel: GiteeAccountsPanel): BaseLoginDialog =
//    if (isOAuthEnabled()) OAuthLoginDialog(project, accountsPanel, accountsPanel::isAccountUnique)
//    else PasswordLoginDialog(project, accountsPanel, accountsPanel::isAccountUnique)
}

private class PasswordLoginDialog(project: Project?, parent: Component?, isAccountUnique: UniqueLoginPredicate) :
  BaseLoginDialog(project, parent, GiteeApiRequestExecutor.Factory.getInstance(), isAccountUnique) {

  init {
    title = message("login.to.gitee")
    setOKButtonText(GitBundle.message("login.dialog.button.login"))
    init()
  }

  override fun createCenterPanel(): JComponent = loginPanel.setPaddingCompensated()

  override fun createSouthAdditionalPanel(): JPanel = createSignUpLink()
}

private class OAuthLoginDialog(project: Project?, parent: Component?, isAccountUnique: UniqueLoginPredicate) :
  BaseLoginDialog(project, parent, GiteeApiRequestExecutor.Factory.getInstance(), isAccountUnique) {

  init {
    title = message("login.to.gitee")
    loginPanel.setOAuthUi()
    init()
  }

  override fun createActions(): Array<Action> = arrayOf(cancelAction)

  override fun show() {
    doOKAction()
    super.show()
  }

  override fun createCenterPanel(): JComponent =
    simplePanel(loginPanel)
      .withPreferredWidth(200)
      .setPaddingCompensated()
}

internal class GEPasswordLoginDialog(project: Project?, parent: Component?, isAccountUnique: UniqueLoginPredicate) :
  BaseLoginDialog(project, parent, GiteeApiRequestExecutor.Factory.getInstance(), isAccountUnique) {

  init {
    title = message("login.to.gitee")
    setOKButtonText(GitBundle.message("login.dialog.button.login"))
    init()
  }

  override fun createCenterPanel(): JComponent = loginPanel.setPaddingCompensated()

  override fun createSouthAdditionalPanel(): JPanel = createSignUpLink()
}

internal class GEOAuthLoginDialog(project: Project?, parent: Component?, isAccountUnique: UniqueLoginPredicate) :
  BaseLoginDialog(project, parent, GiteeApiRequestExecutor.Factory.getInstance(), isAccountUnique) {

  init {
    title = message("login.to.gitee")
    loginPanel.setOAuthUi()
    init()
  }

  override fun createActions(): Array<Action> = arrayOf(cancelAction)

  override fun show() {
    doOKAction()
    super.show()
  }

  override fun createCenterPanel(): JComponent =
    simplePanel(loginPanel)
      .withPreferredWidth(200)
      .setPaddingCompensated()
}