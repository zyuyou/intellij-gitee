/*
 *  Copyright 2016-2022 码云 - Gitee
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

import com.gitee.authentication.accounts.GEAccountManager
import com.gitee.authentication.accounts.GiteeAccount
import com.intellij.collaboration.async.collectWithPrevious
import com.intellij.collaboration.async.disposingMainScope
import com.intellij.collaboration.auth.AccountsListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresEdt
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.annotations.CalledInAny
import java.awt.Component

/**
 * Entry point for interactions with Gitee authentication subsystem
 *
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/authentication/GithubAuthenticationManager.kt
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

  companion object {
    @JvmStatic
    fun getInstance(): GiteeAuthenticationManager = service()
  }
}
