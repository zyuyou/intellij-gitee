// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api

import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.exceptions.GiteeMissingTokenException
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.CalledInAwt
import java.awt.Component

/**
 * Allows to acquire API executor without exposing the auth token to external code
 */
class GiteeApiRequestExecutorManager(private val authenticationManager: GiteeAuthenticationManager,
                                     private val requestExecutorFactory: GiteeApiRequestExecutor.Factory) {
  @CalledInAwt
  fun getExecutor(account: com.gitee.authentication.accounts.GiteeAccount, project: Project): GiteeApiRequestExecutor? {
    return authenticationManager.getOrRequestTokensForAccount(account, project) ?.let { it ->
      requestExecutorFactory.create(it) { authenticationManager.refreshNewTokens(account, it) }
    }
  }

  @CalledInAwt
  fun getExecutor(account: com.gitee.authentication.accounts.GiteeAccount, parentComponent: Component): GiteeApiRequestExecutor? {
    return authenticationManager.getOrRequestTokensForAccount(account, null, parentComponent) ?.let { it ->
      requestExecutorFactory.create(it) { authenticationManager.refreshNewTokens(account, it) }
    }
  }

  @CalledInAwt
  @Throws(com.gitee.exceptions.GiteeMissingTokenException::class)
  fun getExecutor(account: com.gitee.authentication.accounts.GiteeAccount): GiteeApiRequestExecutor {
    return authenticationManager.getTokensForAccount(account)?.let { it ->
      requestExecutorFactory.create(it) { authenticationManager.refreshNewTokens(account, it) }
    } ?: throw com.gitee.exceptions.GiteeMissingTokenException(account)
  }

  companion object {
    @JvmStatic
    fun getInstance(): GiteeApiRequestExecutorManager = service()
  }
}