// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.ui

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeServerPath
import com.gitee.i18n.GiteeBundle.message
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.ui.JBUI.Panels.simplePanel
import git4idea.i18n.GitBundle
import java.awt.Component
import javax.swing.Action
import javax.swing.JComponent

//class AddGEAccountWithTokensAction : BaseAddAccountWithTokensAction() {
//  override val defaultServer: String get() = GiteeServerPath.DEFAULT_HOST
//}
//
//class AddGEEAccountAction : BaseAddAccountWithTokensAction() {
//  override val defaultServer: String get() = ""
//}
//
//abstract class BaseAddAccountWithTokensAction : DumbAwareAction() {
//  abstract val defaultServer: String
//
//  override fun update(e: AnActionEvent) {
//    e.presentation.isEnabledAndVisible = e.getData(GEAccountsHost.KEY) != null
//  }
//
//  override fun actionPerformed(e: AnActionEvent) {
//    val accountsHost = e.getData(GEAccountsHost.KEY)!!
//    val dialog = newAddAccountDialog(e.project, e.getData(PlatformDataKeys.CONTEXT_COMPONENT), accountsHost::isAccountUnique)
//
//    dialog.setServer(defaultServer, defaultServer != GiteeServerPath.DEFAULT_HOST)
//    if (dialog.showAndGet()) {
//      accountsHost.addAccount(dialog.server, dialog.login, dialog.credentials)
//    }
//  }
//}
//
//private fun newAddAccountDialog(project: Project?, parent: Component?, isAccountUnique: UniqueLoginPredicate): BaseLoginDialog =
//  GETokensLoginDialog(project, parent, isAccountUnique).apply {
//    title = message("dialog.title.add.gitee.account")
//    setLoginButtonText(message("accounts.add.button"))
//  }
//
//internal class GETokensLoginDialog(project: Project?, parent: Component?, isAccountUnique: UniqueLoginPredicate) :
//  BaseLoginDialog(project, parent, GiteeApiRequestExecutor.Factory.getInstance(), isAccountUnique) {
//
//  init {
//    title = message("login.to.gitee")
//    setLoginButtonText(GitBundle.message("login.dialog.button.login"))
//    loginPanel.setTokenUi()
//
//    init()
//  }
//
//  internal fun setLoginButtonText(@NlsContexts.Button text: String) = setOKButtonText(text)
//
//  override fun createCenterPanel(): JComponent = loginPanel.setPaddingCompensated()
//}
//
//internal class GERefreshTokensLoginDialog(project: Project?, parent: Component?, isAccountUnique: UniqueLoginPredicate) :
//  BaseLoginDialog(project, parent, GiteeApiRequestExecutor.Factory.getInstance(), isAccountUnique) {
//
//  init {
//    title = message("login.to.gitee")
//    loginPanel.setRefreshTokenUi()
//    init()
//  }
//
//  override fun createActions(): Array<Action> = arrayOf(cancelAction)
//
//  override fun show() {
//    doOKAction()
//    super.show()
//  }
//
//  override fun createCenterPanel(): JComponent =
//    simplePanel(loginPanel)
//      .withPreferredWidth(200)
//      .setPaddingCompensated()
//}