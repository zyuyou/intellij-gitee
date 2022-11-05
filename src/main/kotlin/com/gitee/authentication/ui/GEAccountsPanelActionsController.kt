package com.gitee.authentication.ui

import com.gitee.authentication.GELoginRequest
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GiteeAccount
import com.intellij.collaboration.auth.ui.AccountsPanelActionsController
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import javax.swing.JComponent

internal class GEAccountsPanelActionsController(private val project: Project, private val host: GEAccountsHost) :
  AccountsPanelActionsController<GiteeAccount> {

  private val actionManager = ActionManager.getInstance()

  override val isAddActionWithPopup: Boolean = true

  override fun addAccount(parentComponent: JComponent, point: RelativePoint?) {
    val group = actionManager.getAction("Gitee.Accounts.AddAccount") as ActionGroup

    val actualPoint = point ?: RelativePoint.getCenterOf(parentComponent)
    JBPopupFactory.getInstance()
      .createActionGroupPopup(
        null, group, DataManager.getInstance().getDataContext(parentComponent),
        JBPopupFactory.ActionSelectionAid.MNEMONICS, false
      )
      .show(actualPoint)
  }

  override fun editAccount(parentComponent: JComponent, account: GiteeAccount) {
    val authData = GiteeAuthenticationManager.getInstance().login(
      project, parentComponent,
      GELoginRequest(server = account.server, login = account.name)
    ) ?: return

    account.name = authData.login
    host.updateAccount(authData.account, authData.credentials)
  }
}