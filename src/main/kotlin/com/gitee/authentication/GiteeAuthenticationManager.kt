/*
 * Copyright 2016-2018 码云 - Gitee
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gitee.authentication

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeServerPath
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.accounts.GiteeAccountManager
import com.gitee.authentication.accounts.GiteeProjectDefaultAccountHolder
import com.gitee.authentication.ui.GiteeLoginDialog
import com.gitee.authentication.util.GiteeTokenCreator
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

  @CalledInAwt
  @JvmOverloads
  internal fun getOrRequestTokensForAccount(account: GiteeAccount,
                                            project: Project? = null,
                                            parentComponent: Component? = null): Pair<String, String>? {

    return getTokensForAccount(account) ?: requestNewTokens(account, project, parentComponent)
  }

  @CalledInAny
  internal fun refreshNewTokens(account: GiteeAccount, refreshToken: String) : Triple<GiteeAccount, String, String> {
    val authorization = GiteeTokenCreator(account.server, executorFactory.create(), DumbProgressIndicator()).updateMaster(refreshToken)
    return Triple(account, authorization.accessToken, authorization.refreshToken)
  }

  @CalledInAwt
  private fun requestNewTokens(account: GiteeAccount, project: Project?, parentComponent: Component? = null): Pair<String, String>? {

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

  fun hasTokenForAccount(account: GiteeAccount): Boolean = getTokenForAccount(account) != null

  @CalledInAwt
  @JvmOverloads
  fun requestNewAccount(project: Project?, parentComponent: Component? = null): GiteeAccount? {
    fun isAccountUnique(name: String, server: GiteeServerPath) =
      accountManager.accounts.none { it.name == name && it.server == server }

    val dialog = GiteeLoginDialog(executorFactory, project, parentComponent, ::isAccountUnique)
    DialogManager.show(dialog)
    if (!dialog.isOK) return null

    val account = GiteeAccountManager.createAccount(dialog.getLogin(), dialog.getServer())
    accountManager.accounts += account
    accountManager.updateAccountToken(account, "${dialog.getAccessToken()}&${dialog.getRefreshToken()}")
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
