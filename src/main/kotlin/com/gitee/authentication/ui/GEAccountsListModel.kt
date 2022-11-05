// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.ui

import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GECredentials
import com.gitee.authentication.accounts.GEAccountManager
import com.gitee.authentication.accounts.GiteeAccount
import com.intellij.collaboration.auth.ui.AccountsListModel
import com.intellij.collaboration.auth.ui.MutableAccountsListModel

class GEAccountsListModel : MutableAccountsListModel<GiteeAccount, GECredentials>(),
  AccountsListModel.WithDefault<GiteeAccount, GECredentials>,
  GEAccountsHost {

  override var defaultAccount: GiteeAccount? = null

  override fun addAccount(server: GiteeServerPath, login: String, credentials: GECredentials) {
    val account = GEAccountManager.createAccount(login, server)
    add(account, credentials)
  }

  override fun updateAccount(account: GiteeAccount, credentials: GECredentials) {
    update(account, credentials)
  }

  override fun isAccountUnique(login: String, server: GiteeServerPath): Boolean =
    accountsListModel.items.none { it.name == login && server.equals(server, true) }
}