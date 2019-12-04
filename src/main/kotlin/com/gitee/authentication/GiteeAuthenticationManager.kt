/*
 *  Copyright 2016-2019 码云 - Gitee
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.gitee.authentication

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequests
import com.gitee.api.GiteeServerPath
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.accounts.GiteeAccountManager
import com.gitee.authentication.accounts.GiteeProjectDefaultAccountHolder
import com.gitee.authentication.ui.GiteeLoginDialog
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.project.Project
import git4idea.DialogManager
import org.jetbrains.annotations.CalledInAny
import org.jetbrains.annotations.CalledInAwt
import org.jetbrains.annotations.TestOnly
import java.awt.Component

/**
 * Entry point for interactions with Gitee authentication subsystem
 *
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/authentication/GiteeAuthenticationManager.kt
 * @author JetBrains s.r.o.
 */
class GiteeAuthenticationManager internal constructor(private val accountManager: GiteeAccountManager,
                                                      private val executorFactory: GiteeApiRequestExecutor.Factory) {
  @CalledInAny
  fun hasAccounts(): Boolean = accountManager.accounts.isNotEmpty()

  @CalledInAny
  fun getAccounts(): Set<GiteeAccount> = accountManager.accounts

  @CalledInAny
  internal fun getTokenForAccount(account: GiteeAccount): String? = accountManager.getTokenForAccount(account)

  @CalledInAny
  internal fun getTokensForAccount(account: GiteeAccount): Pair<String, String>? = accountManager.getTokensForAccount(account)

  @CalledInAny
  internal fun getOrRefreshTokensForAccount(account: GiteeAccount): Pair<String, String>? {
    val tokens = accountManager.getTokensForAccount(account) ?: return null

    // no refresh_token return null
    if (tokens.second == "") return null

    executorFactory.create(tokens) {
      newTokens -> accountManager.updateAccountToken(account, "${newTokens.first}&${newTokens.second}")
    }.execute(DumbProgressIndicator(), GiteeApiRequests.CurrentUser.get(account.server))

    return accountManager.getTokensForAccount(account)
  }

  @CalledInAwt
  @JvmOverloads
  internal fun requestNewTokens(account: GiteeAccount, project: Project?, parentComponent: Component? = null): Pair<String, String>? {

    val dialog = GiteeLoginDialog(executorFactory, project, parentComponent, message = "Missing access token for $account")
      .withServer(account.server.toString(), false)
      .withCredentials(account.name)
      .withToken()

    DialogManager.show(dialog)
    if (!dialog.isOK) return null

    account.name = dialog.getLogin()
    accountManager.updateAccountToken(account, "${dialog.getAccessToken()}&${dialog.getRefreshToken()}")

    return dialog.getAccessToken() to dialog.getRefreshToken()
  }

  @CalledInAwt
  @JvmOverloads
  fun requestNewAccount(project: Project?, parentComponent: Component? = null): GiteeAccount? {
    val dialog = GiteeLoginDialog(executorFactory, project, parentComponent, ::isAccountUnique)
    DialogManager.show(dialog)
    if (!dialog.isOK) return null

    return registerAccount(dialog.getLogin(), dialog.getServer(), "${dialog.getAccessToken()}&${dialog.getRefreshToken()}")
  }

  @CalledInAwt
  @JvmOverloads
  fun requestNewAccountForServer(server: GiteeServerPath, project: Project?, parentComponent: Component? = null): GiteeAccount? {
    val dialog = GiteeLoginDialog(executorFactory, project, parentComponent, ::isAccountUnique).withServer(server.toUrl(), false)
    DialogManager.show(dialog)
    if (!dialog.isOK) return null

    return registerAccount(dialog.getLogin(), dialog.getServer(), "${dialog.getAccessToken()}&${dialog.getRefreshToken()}")
  }

  @CalledInAwt
  @JvmOverloads
  fun requestNewAccountForServer(server: GiteeServerPath, login: String, project: Project?, parentComponent: Component? = null): GiteeAccount? {
    val dialog = GiteeLoginDialog(executorFactory, project, parentComponent, ::isAccountUnique)
            .withServer(server.toUrl(), false)
            .withCredentials(login, editableLogin = false)
    DialogManager.show(dialog)
    if (!dialog.isOK) return null

    return registerAccount(dialog.getLogin(), dialog.getServer(), "${dialog.getAccessToken()}&${dialog.getRefreshToken()}")
  }

  internal fun isAccountUnique(name: String, server: GiteeServerPath) = accountManager.accounts.none { it.name == name && it.server == server }

  @CalledInAwt
  internal fun removeAccount(giteeAccount: GiteeAccount) {
    accountManager.accounts -= giteeAccount
  }

  @CalledInAwt
  internal fun updateAccountToken(account: GiteeAccount, newToken: String) {
    accountManager.updateAccountToken(account, newToken)
  }

  private fun registerAccount(name: String, server: GiteeServerPath, token: String): GiteeAccount {
    val account = GiteeAccountManager.createAccount(name, server)
    accountManager.accounts += account
    accountManager.updateAccountToken(account, token)
    return account
  }

  @TestOnly
  fun registerAccount(name: String, host: String, token: String): GiteeAccount {
    val account = GiteeAccountManager.createAccount(name, GiteeServerPath.from(host))
    accountManager.accounts += account
    accountManager.updateAccountToken(account, token)
    return account
  }

  @TestOnly
  fun clearAccounts() {
    for (account in accountManager.accounts) accountManager.updateAccountToken(account, null)
    accountManager.accounts = emptySet()
  }

  fun getDefaultAccount(project: Project): GiteeAccount? {
    return project.service<GiteeProjectDefaultAccountHolder>().account
  }

  @TestOnly
  fun setDefaultAccount(project: Project, account: GiteeAccount?) {
    project.service<GiteeProjectDefaultAccountHolder>().account = account
  }

  @CalledInAwt
  @JvmOverloads
  fun ensureHasAccounts(project: Project?, parentComponent: Component? = null): Boolean {
    if (!hasAccounts()) {
      if (requestNewAccount(project, parentComponent) == null) {
        return false
      }
    }
    return true
  }

  companion object {
    @JvmStatic
    fun getInstance(): GiteeAuthenticationManager {
      return service()
    }
  }
}
