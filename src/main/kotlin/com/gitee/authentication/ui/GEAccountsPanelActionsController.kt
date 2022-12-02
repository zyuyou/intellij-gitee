package com.gitee.authentication.ui

import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GEAccountsUtil
import com.gitee.authentication.GECredentials
import com.gitee.authentication.GELoginRequest
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GEAccountManager
import com.gitee.authentication.accounts.GiteeAccount
import com.intellij.collaboration.auth.ui.AccountsPanelActionsController
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.awt.RelativePoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.JComponent

internal class GEAccountsPanelActionsController(private val project: Project, private val model: GEAccountsListModel) :
  AccountsPanelActionsController<GiteeAccount> {

//  private val actionManager = ActionManager.getInstance()

  override val isAddActionWithPopup: Boolean = true

  override fun addAccount(parentComponent: JComponent, point: RelativePoint?) {
//    val group = actionManager.getAction("Gitee.Accounts.AddAccount") as ActionGroup
//
//    val actualPoint = point ?: RelativePoint.getCenterOf(parentComponent)
//    JBPopupFactory.getInstance()
//      .createActionGroupPopup(
//        null, group, DataManager.getInstance().getDataContext(parentComponent),
//        JBPopupFactory.ActionSelectionAid.MNEMONICS, false
//      )
//      .show(actualPoint)
    val loginModel = AccountsListModelLoginModel(model)
    val group = GEAccountsUtil.createAddAccountActionGroup(loginModel, project, parentComponent)


    val actualPoint = point ?: RelativePoint.getCenterOf(parentComponent)
    JBPopupFactory.getInstance()
      .createActionGroupPopup(null, group, DataManager.getInstance().getDataContext(parentComponent),
        JBPopupFactory.ActionSelectionAid.MNEMONICS, false)
      .show(actualPoint)
  }

  override fun editAccount(parentComponent: JComponent, account: GiteeAccount) {
//    val authData = GiteeAuthenticationManager.getInstance().login(
//      project, parentComponent,
//      GELoginRequest(server = account.server, login = account.name)
//    ) ?: return
//
//    account.name = authData.login
//    host.updateAccount(authData.account, authData.credentials)
    val loginModel = AccountsListModelLoginModel(model, account)
    GEAccountsUtil.login(loginModel,
      GELoginRequest(server = account.server, isServerEditable = false),
      project, parentComponent)
  }

  private class AccountsListModelLoginModel(private val model: GEAccountsListModel,
                                            private val account: GiteeAccount? = null)
    : GELoginModel {

    override fun isAccountUnique(server: GiteeServerPath, login: String): Boolean =
      model.accounts.filter {
        it != account
      }.none {
        it.name == login && it.server.equals(server, true)
      }

    override suspend fun saveLogin(server: GiteeServerPath, login: String, credentials: GECredentials) {
      withContext(Dispatchers.Main) {
        if (account == null) {
          val account = GEAccountManager.createAccount(login, server)
          model.add(account, credentials)
        }
        else {
          account.name = login
          model.update(account, credentials)
        }
      }
    }
  }
}