// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.extensions

import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GEAccountAuthData
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.util.GiteeUtil.GIT_AUTH_PASSWORD_SUBSTITUTE
import com.intellij.openapi.project.Project
import com.intellij.util.AuthData
import com.intellij.util.concurrency.annotations.RequiresEdt
import git4idea.remote.InteractiveGitHttpAuthDataProvider
import java.awt.Component

private val authenticationManager get() = GiteeAuthenticationManager.getInstance()

internal class GECreateAccountHttpAuthDataProvider(
  private val project: Project,
  private val serverPath: GiteeServerPath,
  private val login: String? = null
) : InteractiveGitHttpAuthDataProvider {

  @RequiresEdt
  override fun getAuthData(parentComponent: Component?): AuthData? {
    val account = requestNewAccount(parentComponent) ?: return null
    val token = getOrRequestToken(account, project, parentComponent) ?: return null
    return GEAccountAuthData(account, login ?: GIT_AUTH_PASSWORD_SUBSTITUTE, token)
  }

  private fun requestNewAccount(parentComponent: Component?): GiteeAccount? =
    if (login != null) authenticationManager.requestNewAccountForServer(serverPath, login, project, parentComponent)
    else authenticationManager.requestNewAccountForServer(serverPath, project, parentComponent)

  companion object {
    fun getOrRequestToken(account: GiteeAccount, project: Project, parentComponent: Component?): String? =
      authenticationManager.getTokenForAccount(account) ?: authenticationManager.requestNewTokens(account, project, parentComponent)

//    fun getOrRequestTokens(account: GiteeAccount, project: Project, parentComponent: Component?): Pair<String, String>? =
//      authenticationManager.getTokensForAccount(account) ?: authenticationManager.requestNewTokens(account, project, parentComponent)
  }
}