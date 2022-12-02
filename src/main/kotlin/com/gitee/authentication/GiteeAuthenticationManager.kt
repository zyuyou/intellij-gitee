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

import com.gitee.api.GiteeServerPath
import com.gitee.authentication.accounts.GEAccountManager
import com.gitee.authentication.accounts.GiteeAccount
import com.intellij.collaboration.async.collectWithPrevious
import com.intellij.collaboration.async.disposingMainScope
import com.intellij.collaboration.auth.AccountsListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.AuthData
import com.intellij.util.concurrency.annotations.RequiresEdt
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.annotations.CalledInAny
import java.awt.Component

//internal class GEAccountAuthData(val account: GiteeAccount, login: String, val credentials: GECredentials) : AuthData(login, credentials.accessToken) {
//  val server: GiteeServerPath get() = account.server
//}

/**
 * Entry point for interactions with Gitee authentication subsystem
 *
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/authentication/GiteeAuthenticationManager.kt
 * @author JetBrains s.r.o.
 */
@Deprecated("deprecated in favor of GHAccountsUtil")
class GiteeAuthenticationManager internal constructor() {

  internal val accountManager: GEAccountManager get() = service()

  @CalledInAny
  fun hasAccounts(): Boolean = accountManager.accountsState.value.isNotEmpty()

  @CalledInAny
  fun getAccounts(): Set<GiteeAccount> = accountManager.accountsState.value

  @RequiresEdt
  @JvmOverloads
  fun ensureHasAccounts(project: Project?, parentComponent: Component? = null): Boolean {
    if (accountManager.accountsState.value.isNotEmpty()) return true
    return GEAccountsUtil.requestNewAccount(project = project, parentComponent = parentComponent) != null
  }

  fun getSingleOrDefaultAccount(project: Project): GiteeAccount? =
    GEAccountsUtil.getSingleOrDefaultAccount(project)

  @RequiresEdt
  fun addListener(disposable: Disposable, listener: AccountsListener<GiteeAccount>) =
    disposable.disposingMainScope().launch {
      accountManager.accountsState.collectWithPrevious(setOf()) { prev, current ->
        listener.onAccountListChanged(prev, current)
        current.forEach { acc ->
          async {
            accountManager.getCredentialsFlow(acc).collectLatest {
              listener.onAccountCredentialsChanged(acc)
            }
          }
        }
      }
    }

//  @CalledInAny
//  internal fun getCredentialsForAccount(account: GiteeAccount): GECredentials? =
//    accountManager.findCredentials(account)
//
//  @RequiresEdt
//  @JvmOverloads
//  internal fun requestUpdateCredentials(account: GiteeAccount, expiredCredentials: GECredentials, project: Project?, parentComponent: Component? = null): GECredentials? =
//    login(
//      project, parentComponent,
//      GELoginRequest(
//        text = GiteeBundle.message("account.credentials.update.for", account),
//        server = account.server, login = account.name,
//        credentials = expiredCredentials, isCheckLoginUnique = true
//      )
//    )?.updateAccount(account)
//
//  @RequiresEdt
//  @JvmOverloads
//  internal fun requestNewCredentials(account: GiteeAccount, project: Project?, parentComponent: Component? = null): GECredentials? =
//    login(
//      project, parentComponent,
//      GELoginRequest(
//        text = GiteeBundle.message("account.credentials.missing.for", account),
//        server = account.server, login = account.name
//      )
//    )?.updateAccount(account)
//
//  @RequiresEdt
//  @JvmOverloads
//  fun requestNewAccount(project: Project?, parentComponent: Component? = null): GiteeAccount? =
//    login(
//      project, parentComponent,
//      GELoginRequest(isCheckLoginUnique = true)
//    )?.registerAccount()
//
//  @RequiresEdt
//  @JvmOverloads
//  fun requestNewAccountForServer(server: GiteeServerPath, project: Project?, parentComponent: Component? = null): GiteeAccount? =
//    login(
//      project, parentComponent,
//      GELoginRequest(server = server, isCheckLoginUnique = true)
//    )?.registerAccount()
//
//  @RequiresEdt
//  @JvmOverloads
//  fun requestNewAccountForServer(server: GiteeServerPath, login: String, project: Project?, parentComponent: Component? = null): GiteeAccount? =
//    login(
//      project, parentComponent,
//      GELoginRequest(server = server, login = login, isLoginEditable = false, isCheckLoginUnique = true)
//    )?.registerAccount()
//
//  @RequiresEdt
//  fun requestNewAccountForDefaultServer(project: Project?, useToken: Boolean = false): GiteeAccount? {
//    return GELoginRequest(server = GiteeServerPath.DEFAULT_SERVER, isCheckLoginUnique = true).let {
//      if (!useToken) it.loginWithPassword(project, null) else it.loginWithTokens(project, null)
//    }?.registerAccount()
//  }
//
//  internal fun isAccountUnique(name: String, server: GiteeServerPath) =
//    accountManager.accounts.none { it.name == name && it.server == server }
//
//  @RequiresEdt
//  @JvmOverloads
//  fun requestReLogin(account: GiteeAccount, project: Project?, parentComponent: Component? = null): Boolean =
//    login(
//      project, parentComponent,
//      GELoginRequest(server = account.server, login = account.name)
//    )?.updateAccount(account) != null
//
//  @RequiresEdt
//  internal fun login(project: Project?, parentComponent: Component?, request: GELoginRequest): GEAccountAuthData? =
//    if (request.server?.isGiteeDotCom() == true)
//      if(request.credentials != null) {
//        request.loginRefreshTokens(project, parentComponent)
//      } else {
//        request.loginWithOAuthOrTokens(project, parentComponent)
//      }
//    else
//      if(request.credentials != null) {
//        request.loginRefreshTokens(project, parentComponent)
//      } else {
//        request.loginWithTokens(project, parentComponent)
//      }
//
//  @RequiresEdt
//  internal fun removeAccount(account: GiteeAccount) {
//    accountManager.removeAccount(account)
//  }
//
//  @RequiresEdt
//  internal fun updateAccountCredentials(account: GiteeAccount, newCredentials: GECredentials) {
//    accountManager.updateAccount(account, newCredentials)
//  }
//
//  @RequiresEdt
//  internal fun registerAccount(name: String, server: GiteeServerPath, credentials: GECredentials): GiteeAccount =
//    registerAccount(GEAccountManager.createAccount(name, server), credentials)
//
//  @RequiresEdt
//  internal fun registerAccount(account: GiteeAccount, credentials: GECredentials): GiteeAccount {
//    accountManager.updateAccount(account, credentials)
//    return account
//  }
//
//  @TestOnly
//  fun clearAccounts() {
//    accountManager.updateAccounts(emptyMap())
//  }
//
//  fun getDefaultAccount(project: Project): GiteeAccount? =
//    project.service<GiteeProjectDefaultAccountHolder>().account
//
//  @TestOnly
//  fun setDefaultAccount(project: Project, account: GiteeAccount?) {
//    project.service<GiteeProjectDefaultAccountHolder>().account = account
//  }

  companion object {
    @JvmStatic
    fun getInstance(): GiteeAuthenticationManager = service()
  }
}

//private fun GEAccountAuthData.registerAccount(): GiteeAccount =
//  GiteeAuthenticationManager.getInstance().registerAccount(login, server, credentials)
//
//private fun GEAccountAuthData.updateAccount(account: GiteeAccount): GECredentials {
//  account.name = login
//  GiteeAuthenticationManager.getInstance().updateAccountCredentials(account, credentials)
//  return credentials
//}
