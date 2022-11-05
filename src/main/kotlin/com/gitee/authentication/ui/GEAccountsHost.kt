// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.ui

import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GECredentials
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.i18n.GiteeBundle.message
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.JBPopupFactory.ActionSelectionAid
import com.intellij.ui.components.DropDownLink
import javax.swing.JButton

internal interface GEAccountsHost {
  fun addAccount(server: GiteeServerPath, login: String, credentials: GECredentials)
  fun updateAccount(account: GiteeAccount, credentials: GECredentials)
  fun isAccountUnique(login: String, server: GiteeServerPath): Boolean

  companion object {
    val KEY: DataKey<GEAccountsHost> = DataKey.create("GEAccountsHost")

    fun createAddAccountLink(): JButton =
      DropDownLink(message("accounts.add.dropdown.link")) {
        val group = ActionManager.getInstance().getAction("Gitee.Accounts.AddAccount") as ActionGroup
        val dataContext = DataManager.getInstance().getDataContext(it)

        JBPopupFactory.getInstance().createActionGroupPopup(null, group, dataContext, ActionSelectionAid.MNEMONICS, false)
      }
  }
}