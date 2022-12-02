// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.extensions

import com.gitee.authentication.GEAccountAuthData
import com.gitee.authentication.GEAccountsUtil
import com.gitee.authentication.GECredentials
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.ui.GiteeChooseAccountDialog
import com.gitee.i18n.GiteeBundle
import com.intellij.openapi.project.Project
import com.intellij.util.AuthData
import com.intellij.util.concurrency.annotations.RequiresEdt
import git4idea.DialogManager
import git4idea.i18n.GitBundle
import git4idea.remote.InteractiveGitHttpAuthDataProvider
import java.awt.Component

internal class GESelectAccountHttpAuthDataProvider(
  private val project: Project,
  private val potentialAccounts: Map<GiteeAccount, GECredentials?>
) : InteractiveGitHttpAuthDataProvider {

  @RequiresEdt
  override fun getAuthData(parentComponent: Component?): AuthData? {
    val (account, setDefault) = chooseAccount(parentComponent) ?: return null
    val credentials = potentialAccounts[account]
      ?: GEAccountsUtil.requestNewCredentials(account, project, parentComponent)
      ?: return null
    if (setDefault) {
      GEAccountsUtil.setDefaultAccount(project, account)
    }

    return GEAccountAuthData(account, account.name, credentials)
  }

  private fun chooseAccount(parentComponent: Component?): Pair<GiteeAccount, Boolean>? {
    val dialog = GiteeChooseAccountDialog(
      project, parentComponent,
      potentialAccounts.keys, null, false, true,
      GiteeBundle.message("account.choose.title"), GitBundle.message("login.dialog.button.login")
    )
    DialogManager.show(dialog)

    return if (dialog.isOK) dialog.account to dialog.setDefault else null
  }
}