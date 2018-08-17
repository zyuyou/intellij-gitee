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
package com.gitee.api

import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.exceptions.GiteeMissingTokenException
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.CalledInAwt
import java.awt.Component

/**
 * Allows to acquire API executor without exposing the auth token to external code
 *
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/api/GithubApiRequestExecutorManager.kt
 * @author JetBrains s.r.o.
 */
class GiteeApiRequestExecutorManager(private val authenticationManager: GiteeAuthenticationManager,
                                     private val requestExecutorFactory: GiteeApiRequestExecutor.Factory) {
  @CalledInAwt
  fun getExecutor(account: GiteeAccount, project: Project): GiteeApiRequestExecutor? {
    return authenticationManager.getOrRequestTokensForAccount(account, project) ?.let { it ->
      requestExecutorFactory.create(it) { authenticationManager.refreshNewTokens(account, it) }
    }
  }

  @CalledInAwt
  fun getExecutor(account: GiteeAccount, parentComponent: Component): GiteeApiRequestExecutor? {
    return authenticationManager.getOrRequestTokensForAccount(account, null, parentComponent) ?.let { it ->
      requestExecutorFactory.create(it) { authenticationManager.refreshNewTokens(account, it) }
    }
  }

  @CalledInAwt
  @Throws(GiteeMissingTokenException::class)
  fun getExecutor(account: GiteeAccount): GiteeApiRequestExecutor {
    return authenticationManager.getTokensForAccount(account)?.let { it ->
      requestExecutorFactory.create(it) { authenticationManager.refreshNewTokens(account, it) }
    } ?: throw GiteeMissingTokenException(account)
  }

  companion object {
    @JvmStatic
    fun getInstance(): GiteeApiRequestExecutorManager = service()
  }
}