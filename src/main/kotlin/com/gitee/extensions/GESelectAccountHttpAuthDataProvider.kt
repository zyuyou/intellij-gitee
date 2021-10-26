// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.extensions

import com.gitee.authentication.GEAccountAuthData
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.ui.GiteeChooseAccountDialog
import com.gitee.extensions.GECreateAccountHttpAuthDataProvider.Companion.getOrRequestToken
import com.gitee.i18n.GiteeBundle
import com.gitee.util.GiteeUtil.GIT_AUTH_PASSWORD_SUBSTITUTE
import com.intellij.openapi.project.Project
import com.intellij.util.AuthData
import com.intellij.util.concurrency.annotations.RequiresEdt
import git4idea.DialogManager
import git4idea.i18n.GitBundle
import git4idea.remote.InteractiveGitHttpAuthDataProvider
import java.awt.Component

internal class GESelectAccountHttpAuthDataProvider(
  private val project: Project,
  private val potentialAccounts: Collection<GiteeAccount>
) : InteractiveGitHttpAuthDataProvider {

  @RequiresEdt
  override fun getAuthData(parentComponent: Component?): AuthData? {
    val (account, setDefault) = chooseAccount(parentComponent) ?: return null
    val token = getOrRequestToken(account, project, parentComponent) ?: return null
    if (setDefault) GiteeAuthenticationManager.getInstance().setDefaultAccount(project, account)

    return GEAccountAuthData(account, GIT_AUTH_PASSWORD_SUBSTITUTE, token)
  }

  private fun chooseAccount(parentComponent: Component?): Pair<GiteeAccount, Boolean>? {
    val dialog = GiteeChooseAccountDialog(
      project, parentComponent,
      potentialAccounts, null, false, true,
      GiteeBundle.message("account.choose.title"), GitBundle.message("login.dialog.button.login")
    )
    DialogManager.show(dialog)

    return if (dialog.isOK) dialog.account to dialog.setDefault else null
  }
}