// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.ui

import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GECredentials
import com.gitee.authentication.GELoginRequest
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GEAccountManager
import com.gitee.authentication.accounts.GiteeAccount
import com.intellij.collaboration.auth.ui.AccountsListModel
import com.intellij.collaboration.auth.ui.AccountsListModelBase
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.JBPopupMenu
import com.intellij.ui.awt.RelativePoint
import javax.swing.JComponent

class GEAccountsListModel(private val project: Project)
  : AccountsListModelBase<GiteeAccount, GECredentials>(),
    AccountsListModel.WithDefault<GiteeAccount, GECredentials>,
    GEAccountsHost {

  private val actionManager = ActionManager.getInstance()

  override var defaultAccount: GiteeAccount? = null

  override fun addAccount(parentComponent: JComponent, point: RelativePoint?) {
    val group = actionManager.getAction("Gitee.Accounts.AddAccount") as ActionGroup
    val popup = actionManager.createActionPopupMenu(ActionPlaces.UNKNOWN, group)

    val actualPoint = point ?: RelativePoint.getCenterOf(parentComponent)
    popup.setTargetComponent(parentComponent)
    JBPopupMenu.showAt(actualPoint, popup.component)
  }

  override fun editAccount(parentComponent: JComponent, account: GiteeAccount) {
    val authData = GiteeAuthenticationManager.getInstance().login(
      project, parentComponent,
      GELoginRequest(server = account.server, login = account.name)
    ) ?: return

    account.name = authData.login
    newCredentials[account] = authData.credentials
    notifyCredentialsChanged(account)
  }

  override fun addAccount(server: GiteeServerPath, login: String, credentials: GECredentials) {
    val account = GEAccountManager.createAccount(login, server)
    accountsListModel.add(account)
    newCredentials[account] = credentials
    notifyCredentialsChanged(account)
  }

  override fun isAccountUnique(login: String, server: GiteeServerPath): Boolean =
    accountsListModel.items.none { it.name == login && it.server.equals(server, true) }
}