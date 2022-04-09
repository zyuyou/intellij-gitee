// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.accounts

import com.gitee.api.GiteeServerPath
import com.gitee.util.GiteeUtil
import com.intellij.collaboration.auth.AccountManagerBase
import com.intellij.collaboration.auth.AccountsListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.util.messages.Topic
import org.jetbrains.annotations.ApiStatus

internal val GiteeAccount.isGEAccount: Boolean get() = server.isGiteeDotCom()

/**
 * Handles application-level Gitee accounts
 */
@Service
internal class GEAccountManager
  : AccountManagerBase<GiteeAccount, String>(GiteeUtil.SERVICE_DISPLAY_NAME) {

  override fun accountsRepository() = service<GEPersistentAccounts>()

  override fun serializeCredentials(credentials: String): String = credentials
  override fun deserializeCredentials(credentials: String): String = credentials

  fun findCredentialsPair(account: GiteeAccount): Pair<String, String>? {
    return super.findCredentials(account)?.let { credentials ->
      credentials.split("&").let { credentialsList ->
        if (credentialsList.size == 1) Pair(credentialsList[0], "") else Pair(credentialsList[0], credentialsList[1])
      }
    }
  }

  init {
    @Suppress("DEPRECATION")
    addListener(this, object : AccountsListener<GiteeAccount> {
      override fun onAccountListChanged(old: Collection<GiteeAccount>, new: Collection<GiteeAccount>) {
        val removedPublisher = ApplicationManager.getApplication().messageBus.syncPublisher(ACCOUNT_REMOVED_TOPIC)
        for (account in (old - new)) {
          removedPublisher.accountRemoved(account)
        }
        val tokenPublisher = ApplicationManager.getApplication().messageBus.syncPublisher(ACCOUNT_TOKEN_CHANGED_TOPIC)
        for (account in (new - old)) {
          tokenPublisher.tokenChanged(account)
        }
      }

      override fun onAccountCredentialsChanged(account: GiteeAccount) =
        ApplicationManager.getApplication().messageBus.syncPublisher(ACCOUNT_TOKEN_CHANGED_TOPIC).tokenChanged(account)
    })
  }

  companion object {
    @Deprecated("Use TOPIC")
    @Suppress("DEPRECATION")
    @JvmStatic
    val ACCOUNT_REMOVED_TOPIC = Topic("GITEE_ACCOUNT_REMOVED", AccountRemovedListener::class.java)

    @Deprecated("Use TOPIC")
    @Suppress("DEPRECATION")
    @JvmStatic
    val ACCOUNT_TOKEN_CHANGED_TOPIC = Topic("GITEE_ACCOUNT_TOKEN_CHANGED", AccountTokenChangedListener::class.java)

    fun createAccount(name: String, server: GiteeServerPath) = GiteeAccount(name, server)
  }
}

@Deprecated("Use GiteeAuthenticationManager.addListener")
@ApiStatus.ScheduledForRemoval
interface AccountRemovedListener {
  fun accountRemoved(removedAccount: GiteeAccount)
}

@Deprecated("Use GiteeAuthenticationManager.addListener")
@ApiStatus.ScheduledForRemoval
interface AccountTokenChangedListener {
  fun tokenChanged(account: GiteeAccount)
}