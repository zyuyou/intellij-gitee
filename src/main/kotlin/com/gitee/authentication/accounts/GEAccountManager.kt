// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.accounts

import com.fasterxml.jackson.databind.DeserializationFeature
import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GECredentials
import com.gitee.authentication.accounts.GEAccountsUtils.jacksonMapper
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
  : AccountManagerBase<GiteeAccount, GECredentials>(GiteeUtil.SERVICE_DISPLAY_NAME) {

  override fun accountsRepository() = service<GEPersistentAccounts>()

  override fun serializeCredentials(credentials: GECredentials): String =
    jacksonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(credentials)

  override fun deserializeCredentials(credentials: String): GECredentials {
    try {
      return jacksonMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(credentials, GECredentials::class.java)
    } catch (ignore: Exception) {
      return GECredentials.EmptyCredentials
    }
  }


  fun findCredentialsPair(account: GiteeAccount): Pair<String, String>? {
    return super.findCredentials(account)?.let { credentials ->
      Pair(credentials.accessToken, credentials.refreshToken)
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