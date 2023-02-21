// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.ui

import com.gitee.authentication.GECredentials
import com.gitee.authentication.accounts.GiteeAccount
import com.intellij.collaboration.auth.ui.AccountsListModel
import com.intellij.collaboration.auth.ui.MutableAccountsListModel

class GEAccountsListModel : MutableAccountsListModel<GiteeAccount, GECredentials>(),
  AccountsListModel.WithDefault<GiteeAccount, GECredentials> {

  override var defaultAccount: GiteeAccount? = null
}