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
import com.gitee.authentication.accounts.GEAccountManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.accounts.GiteeProjectDefaultAccountHolder
import com.gitee.i18n.GiteeBundle
import com.intellij.collaboration.auth.AccountsListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.util.AuthData
import com.intellij.util.concurrency.annotations.RequiresEdt
import org.jetbrains.annotations.CalledInAny
import org.jetbrains.annotations.TestOnly
import java.awt.Component

internal class GEAccountAuthData(val account: GiteeAccount, login: String, token: String) : AuthData(login, token) {
  val server: GiteeServerPath get() = account.server
  val token: String get() = password!!
}

/**
 * Entry point for interactions with Gitee authentication subsystem
 *
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/authentication/GiteeAuthenticationManager.kt
 * @author JetBrains s.r.o.
 */
class GiteeAuthenticationManager internal constructor() {

  private val accountManager: GEAccountManager
    get() = service()

  private val executorFactory: GiteeApiRequestExecutor.Factory
    get() = service()

  @CalledInAny
  fun hasAccounts(): Boolean = accountManager.accounts.isNotEmpty()

  @CalledInAny
  fun getAccounts(): Set<GiteeAccount> = accountManager.accounts

  @CalledInAny
  internal fun getTokenForAccount(account: GiteeAccount): String? = accountManager.findCredentials(account)

  @CalledInAny
  internal fun getTokensForAccount(account: GiteeAccount): Pair<String, String>? = accountManager.findCredentialsPair(account)

  @CalledInAny
  internal fun getOrRefreshTokensForAccount(account: GiteeAccount): Pair<String, String>? {
    val tokens = accountManager.findCredentialsPair(account) ?: return null

    // no refresh_token return null
    if (tokens.second == "") return null

    executorFactory.create(tokens) {
      newTokens -> accountManager.updateAccount(account, "${newTokens.first}&${newTokens.second}")
    }.execute(DumbProgressIndicator(), GiteeApiRequests.CurrentUser.get(account.server))

    return accountManager.findCredentialsPair(account)
  }

  @RequiresEdt
  @JvmOverloads
//  internal fun requestNewTokens(account: GiteeAccount, project: Project?, parentComponent: Component? = null): Pair<String, String>? {
//
//    val dialog = GiteeLoginDialog(executorFactory, project, parentComponent, message = GiteeBundle.message("account.token.missing.for", account),)
//      .withServer(account.server.toString(), false)
//      .withCredentials(account.name)
//      .withToken()
//
//    DialogManager.show(dialog)
//    if (!dialog.isOK) return null
//
//    account.name = dialog.login
//    accountManager.updateAccount(account, "${dialog.accessToken}&${dialog.refreshToken}")
//
//    return dialog.accessToken to dialog.refreshToken
//  }
  internal fun requestNewTokens(account: GiteeAccount, project: Project?, parentComponent: Component? = null): String? =
    login(
      project, parentComponent,
      GELoginRequest(
        text = GiteeBundle.message("account.token.missing.for", account),
        server = account.server, login = account.name
      )
    )?.updateAccount(account)

  @RequiresEdt
  @JvmOverloads
//  fun requestNewAccount(project: Project?, parentComponent: Component? = null): GiteeAccount? {
//    val dialog = GiteeLoginDialog(executorFactory, project, parentComponent, ::isAccountUnique)
//    DialogManager.show(dialog)
//    if (!dialog.isOK) return null
//
//    return registerAccount(dialog.login, dialog.server, "${dialog.accessToken}&${dialog.refreshToken}")
//  }
  fun requestNewAccount(project: Project?, parentComponent: Component? = null): GiteeAccount? =
    login(
      project, parentComponent,
      GELoginRequest(isCheckLoginUnique = true)
    )?.registerAccount()

  @RequiresEdt
  @JvmOverloads
//  fun requestNewAccountForServer(server: GiteeServerPath, project: Project?, parentComponent: Component? = null): GiteeAccount? {
//    val dialog = GiteeLoginDialog(executorFactory, project, parentComponent, ::isAccountUnique).withServer(server.toUrl(), false)
//    DialogManager.show(dialog)
//    if (!dialog.isOK) return null
//
//    return registerAccount(dialog.login, dialog.server, "${dialog.accessToken}&${dialog.refreshToken}")
//  }
  fun requestNewAccountForServer(server: GiteeServerPath, project: Project?, parentComponent: Component? = null): GiteeAccount? =
    login(
      project, parentComponent,
      GELoginRequest(server = server, isCheckLoginUnique = true)
    )?.registerAccount()

  @RequiresEdt
  @JvmOverloads
//  fun requestNewAccountForServer(server: GiteeServerPath, login: String, project: Project?, parentComponent: Component? = null): GiteeAccount? {
//    val dialog = GiteeLoginDialog(executorFactory, project, parentComponent, ::isAccountUnique)
//            .withServer(server.toUrl(), false)
//            .withCredentials(login, editableLogin = false)
//    DialogManager.show(dialog)
//    if (!dialog.isOK) return null
//
//    return registerAccount(dialog.login, dialog.server, "${dialog.accessToken}&${dialog.refreshToken}")
//  }
  fun requestNewAccountForServer(server: GiteeServerPath, login: String, project: Project?, parentComponent: Component? = null): GiteeAccount? =
    login(
      project, parentComponent,
      GELoginRequest(server = server, login = login, isLoginEditable = false, isCheckLoginUnique = true)
    )?.registerAccount()

  @RequiresEdt
  fun requestNewAccountForDefaultServer(project: Project?, useToken: Boolean = false): GiteeAccount? {
    return GELoginRequest(server = GiteeServerPath.DEFAULT_SERVER, isCheckLoginUnique = true).let {
      if (!useToken) it.loginWithPassword(project, null) else it.loginWithTokens(project, null)
    }?.registerAccount()
  }

  internal fun isAccountUnique(name: String, server: GiteeServerPath) =
    accountManager.accounts.none { it.name == name && it.server == server }

//  @CalledInAwt
//  @JvmOverloads
//  fun requestReLogin(account: GiteeAccount, project: Project?, parentComponent: Component? = null): Boolean {
//    val dialog = GiteeLoginDialog(GiteeApiRequestExecutor.Factory.getInstance(), project, parentComponent)
//      .withServer(account.server.toString(), false)
//      .withCredentials(account.name)
//
//    DialogManager.show(dialog)
//    if (!dialog.isOK) return false
//
//    val token = dialog.token
//    account.name = dialog.login
//    accountManager.updateAccountToken(account, token)
//    return true
//  }

  @RequiresEdt
  @JvmOverloads
  fun requestReLogin(account: GiteeAccount, project: Project?, parentComponent: Component? = null): Boolean =
    login(
      project, parentComponent,
      GELoginRequest(server = account.server, login = account.name)
    )?.updateAccount(account) != null

  @RequiresEdt
  internal fun login(project: Project?, parentComponent: Component?, request: GELoginRequest): GEAccountAuthData? =
    if (request.server?.isGiteeDotCom() == true)
      request.loginWithOAuthOrTokens(project, parentComponent)
    else
      request.loginWithTokens(project, parentComponent)

  @RequiresEdt
  internal fun removeAccount(account: GiteeAccount) {
    accountManager.removeAccount(account)
  }

  @RequiresEdt
  internal fun updateAccountToken(account: GiteeAccount, newToken: String) {
    accountManager.updateAccount(account, newToken)
  }

  @RequiresEdt
  internal fun registerAccount(name: String, server: GiteeServerPath, token: String): GiteeAccount =
    registerAccount(GEAccountManager.createAccount(name, server), token)

  @RequiresEdt
  internal fun registerAccount(account: GiteeAccount, token: String): GiteeAccount {
    accountManager.updateAccount(account, token)
    return account
  }

//  private fun registerAccount(name: String, server: GiteeServerPath, token: String): GiteeAccount {
//    val account = GiteeAccountManager.createAccount(name, server)
//    accountManager.accounts += account
//    accountManager.updateAccountToken(account, token)
//    return account
//  }

//  @TestOnly
//  fun registerAccount(name: String, host: String, token: String): GiteeAccount {
//    val account = GiteeAccountManager.createAccount(name, GiteeServerPath.from(host))
//    accountManager.accounts += account
//    accountManager.updateAccountToken(account, token)
//    return account
//  }

  @TestOnly
  fun clearAccounts() {
    accountManager.updateAccounts(emptyMap())
  }
//  @TestOnly
//  fun clearAccounts() {
//    for (account in accountManager.accounts) accountManager.updateAccountToken(account, null)
//    accountManager.accounts = emptySet()
//  }

  fun getDefaultAccount(project: Project): GiteeAccount? =
    project.service<GiteeProjectDefaultAccountHolder>().account

  @TestOnly
  fun setDefaultAccount(project: Project, account: GiteeAccount?) {
    project.service<GiteeProjectDefaultAccountHolder>().account = account
  }

  @RequiresEdt
  @JvmOverloads
  fun ensureHasAccounts(project: Project?, parentComponent: Component? = null): Boolean =
    hasAccounts() || requestNewAccount(project, parentComponent) != null

//  fun getSingleOrDefaultAccount(project: Project): GiteeAccount? {
//    project.service<GiteeProjectDefaultAccountHolder>().account?.let { return it }
//    val accounts = accountManager.accounts
//    if (accounts.size == 1) return accounts.first()
//    return null
//  }
  fun getSingleOrDefaultAccount(project: Project): GiteeAccount? =
    project.service<GiteeProjectDefaultAccountHolder>().account
      ?: accountManager.accounts.singleOrNull()

  @RequiresEdt
  fun addListener(disposable: Disposable, listener: AccountsListener<GiteeAccount>) =
    accountManager.addListener(disposable, listener)

  companion object {
    @JvmStatic
    fun getInstance(): GiteeAuthenticationManager {
      return service()
    }
  }
}

private fun GEAccountAuthData.registerAccount(): GiteeAccount =
  GiteeAuthenticationManager.getInstance().registerAccount(login, server, token)

private fun GEAccountAuthData.updateAccount(account: GiteeAccount): String {
  account.name = login
  GiteeAuthenticationManager.getInstance().updateAccountToken(account, token)
  return token
}
