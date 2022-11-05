// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.accounts

import com.fasterxml.jackson.databind.DeserializationFeature
import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GECredentials
import com.gitee.authentication.accounts.GEAccountsUtils.jacksonMapper
import com.gitee.util.GiteeUtil
import com.intellij.collaboration.auth.AccountManagerBase
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
    return try {
      jacksonMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).readValue(credentials, GECredentials::class.java)
    } catch (ignore: Exception) {
      GECredentials.EmptyCredentials
    }
  }

  val dummyAccountsState: StateFlow<Map<GiteeAccount, String?>> = MutableStateFlow(accounts.associateWith { "" }).asStateFlow()

  companion object {
    fun createAccount(name: String, server: GiteeServerPath) = GiteeAccount(name, server)
  }
}