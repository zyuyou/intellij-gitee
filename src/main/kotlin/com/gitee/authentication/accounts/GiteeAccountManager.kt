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
package com.gitee.authentication.accounts

import com.gitee.api.GiteeServerPath
import com.gitee.util.GiteeUtil
import com.intellij.credentialStore.*
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.messages.Topic
import kotlin.properties.Delegates.observable

//internal val GiteeAccount.isGiteeAccount: Boolean get() = server.isGiteeDotCom()

/**
 * Handles application-level Gitee accounts
 *
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/authentication/accounts/GithubAccountManager.kt
 * @author JetBrains s.r.o.
 */
@State(name = "GiteeAccounts", storages = [(Storage("gitee.xml"))])
internal class GiteeAccountManager() : PersistentStateComponent<Array<GiteeAccount>> {

  var accounts: Set<GiteeAccount> by observable(setOf()) {
    _, oldValue, newValue ->
    oldValue.filter { it !in newValue }.forEach(this::accountRemoved)
    LOG.debug("Account list changed to: " + newValue.toString())
  }

  init {
    ApplicationManager.getApplication().messageBus.connect()
      .subscribe(PasswordSafeSettings.TOPIC, object : PasswordSafeSettingsListener {
        override fun credentialStoreCleared() {
          val publisher = ApplicationManager.getApplication().messageBus.syncPublisher(ACCOUNT_TOKEN_CHANGED_TOPIC)
          accounts.forEach(publisher::tokenChanged)
        }
      })
  }

  private fun accountRemoved(account: GiteeAccount) {
    updateAccountToken(account, null)
    ApplicationManager.getApplication().messageBus.syncPublisher(ACCOUNT_REMOVED_TOPIC).accountRemoved(account)
  }

  /**
   * Add/update/remove Gitee OAuth token from application
   */
  fun updateAccountToken(account: GiteeAccount, token: String?) {
    PasswordSafe.instance.set(createCredentialAttributes(account.id), token?.let { createCredentials(account.id, it) })
    LOG.debug((if (token == null) "Cleared" else "Updated") + " OAuth token for account: $account")
    ApplicationManager.getApplication().messageBus.syncPublisher(ACCOUNT_TOKEN_CHANGED_TOPIC).tokenChanged(account)
  }

  /**
   * Retrieve OAuth token for account from password safe
   */
  fun getTokenForAccount(account: GiteeAccount): String? = getTokensForAccount(account)?.first

  fun getTokensForAccount(account: GiteeAccount): Pair<String, String>? {
    return PasswordSafe.instance.get(createCredentialAttributes(account.id))?.let {
      credential ->
        credential.getPasswordAsString()?.let {
          tokens ->
            tokens.split("&").let {
              tokenList ->
//              "79b4aacddf990b49980215fab2a4304d" expired token for test
                if (tokenList.size == 1) Pair(tokenList[0], "") else Pair(tokenList[0], tokenList[1])
            }
        }
    }
  }

  override fun getState() = accounts.toTypedArray()

  override fun loadState(state: Array<GiteeAccount>) {
    accounts = state.toHashSet()
  }

  companion object {
    private val LOG = Logger.getInstance(GiteeAccountManager::class.java)

    @JvmStatic
    val ACCOUNT_REMOVED_TOPIC = Topic("GITEE_ACCOUNT_REMOVED", AccountRemovedListener::class.java)

    @JvmStatic
    val ACCOUNT_TOKEN_CHANGED_TOPIC = Topic("GITEE_ACCOUNT_TOKEN_CHANGED", AccountTokenChangedListener::class.java)

    fun createAccount(name: String, server: GiteeServerPath) = GiteeAccount(name, server)
  }
}

private fun createCredentialAttributes(accountId: String) = CredentialAttributes(createServiceName(accountId))

private fun createCredentials(accountId: String, token: String) = Credentials(accountId, token)

private fun createServiceName(accountId: String): String = generateServiceName(GiteeUtil.SERVICE_DISPLAY_NAME, accountId)

//interface AccountRemovedListener {
//  fun accountRemoved(removedAccount: GiteeAccount)
//}
//
//interface AccountTokenChangedListener {
//  fun tokenChanged(account: GiteeAccount)
//}