// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.accounts

import com.intellij.credentialStore.*
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.messages.Topic
import com.gitee.api.GiteeServerPath
import com.gitee.util.GiteeUtil
import kotlin.properties.Delegates.observable

/**
 * Handles application-level Gitee accounts
 */
@State(name = "GiteeAccounts", storages = [(Storage("gitee_settings.xml"))])
internal class GiteeAccountManager(private val passwordSafe: PasswordSafe) : PersistentStateComponent<Array<com.gitee.authentication.accounts.GiteeAccount>> {

  var accounts: Set<com.gitee.authentication.accounts.GiteeAccount> by observable(setOf()) {
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

  private fun accountRemoved(account: com.gitee.authentication.accounts.GiteeAccount) {
    updateAccountToken(account, null)
    ApplicationManager.getApplication().messageBus.syncPublisher(ACCOUNT_REMOVED_TOPIC).accountRemoved(account)
  }

  /**
   * Add/update/remove Gitee OAuth token from application
   */
  fun updateAccountToken(account: com.gitee.authentication.accounts.GiteeAccount, token: String?) {
    passwordSafe.set(createCredentialAttributes(account.id), token?.let { createCredentials(account.id, it) })
    LOG.debug((if (token == null) "Cleared" else "Updated") + " OAuth token for account: $account")
    ApplicationManager.getApplication().messageBus.syncPublisher(ACCOUNT_TOKEN_CHANGED_TOPIC).tokenChanged(account)
  }

  /**
   * Retrieve OAuth token for account from password safe
   */
  fun getTokenForAccount(account: com.gitee.authentication.accounts.GiteeAccount): String? = getTokensForAccount(account)?.first

  fun getTokensForAccount(account: com.gitee.authentication.accounts.GiteeAccount): Pair<String, String>? {
    return passwordSafe.get(createCredentialAttributes(account.id))?.let {
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

  override fun loadState(state: Array<com.gitee.authentication.accounts.GiteeAccount>) {
    accounts = state.toHashSet()
  }

  companion object {
    private val LOG = Logger.getInstance(GiteeAccountManager::class.java)

    @JvmStatic
    val ACCOUNT_REMOVED_TOPIC = Topic("GITEE_ACCOUNT_REMOVED", AccountRemovedListener::class.java)

    @JvmStatic
    val ACCOUNT_TOKEN_CHANGED_TOPIC = Topic("GITEE_ACCOUNT_TOKEN_CHANGED", AccountTokenChangedListener::class.java)

    fun createAccount(name: String, server: GiteeServerPath) = com.gitee.authentication.accounts.GiteeAccount(name, server)
  }
}

private fun createCredentialAttributes(accountId: String) = CredentialAttributes(createServiceName(accountId))

private fun createCredentials(accountId: String, token: String) = Credentials(accountId, token)

private fun createServiceName(accountId: String): String = generateServiceName(GiteeUtil.SERVICE_DISPLAY_NAME, accountId)

interface AccountRemovedListener {
  fun accountRemoved(removedAccount: com.gitee.authentication.accounts.GiteeAccount)
}

interface AccountTokenChangedListener {
  fun tokenChanged(account: com.gitee.authentication.accounts.GiteeAccount)
}