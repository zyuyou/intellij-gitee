// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.ui

import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GECredentials
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GiteeAccount
import com.intellij.ui.CollectionComboBoxModel

//internal class GEAccountsComboBoxModel(accounts: Set<GiteeAccount>, selection: GiteeAccount?) :
//  CollectionComboBoxModel<GiteeAccount>(accounts.toMutableList(), selection),
//  GEAccountsHost {
//
//  override fun addAccount(server: GiteeServerPath, login: String, credentials: GECredentials) {
//    val account = GiteeAuthenticationManager.getInstance().registerAccount(login, server, credentials)
//
//    add(account)
//    selectedItem = account
//  }
//
//  override fun updateAccount(account: GiteeAccount, credentials: GECredentials) {
//    GiteeAuthenticationManager.getInstance().updateAccountCredentials(account, credentials)
//  }
//
//  override fun isAccountUnique(login: String, server: GiteeServerPath): Boolean =
//    GiteeAuthenticationManager.getInstance().isAccountUnique(login, server)
//}