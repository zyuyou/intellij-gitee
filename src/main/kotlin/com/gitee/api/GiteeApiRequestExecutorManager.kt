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
package com.gitee.api

import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.AccountTokenChangedListener
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.accounts.GiteeAccountManager
import com.gitee.exceptions.GiteeMissingTokenException
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.CalledInAwt
import java.awt.Component

/**
 * Allows to acquire API executor without exposing the auth token to external code
 *
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/api/GiteeApiRequestExecutorManager.kt
 * @author JetBrains s.r.o.
 */
class GiteeApiRequestExecutorManager internal constructor(private val accountManager: GiteeAccountManager,
                                                          private val authenticationManager: GiteeAuthenticationManager,
                                                          private val requestExecutorFactory: GiteeApiRequestExecutor.Factory) : AccountTokenChangedListener {

  private val executors = mutableMapOf<GiteeAccount, GiteeApiRequestExecutor.WithTokensAuth>()

  init {
    ApplicationManager.getApplication().messageBus
      .connect()
      .subscribe(GiteeAccountManager.ACCOUNT_TOKEN_CHANGED_TOPIC, this)
  }

  override fun tokenChanged(account: GiteeAccount) {
    val tokens = accountManager.getTokensForAccount(account)
    if (tokens == null) executors.remove(account) else executors[account]?.tokens = tokens
  }

//  @CalledInAwt
//  fun getExecutor(account: GiteeAccount, project: Project): GiteeApiRequestExecutor? {
//    return authenticationManager.getOrRequestTokensForAccount(account, project)?.let { it ->
//      requestExecutorFactory.create(it) { authenticationManager.refreshNewTokens(account, it) }
//    }
//  }
//
//  @CalledInAwt
//  fun getExecutor(account: GiteeAccount, parentComponent: Component): GiteeApiRequestExecutor? {
//    return authenticationManager.getOrRequestTokensForAccount(account, null, parentComponent)?.let { it ->
//      requestExecutorFactory.create(it) { authenticationManager.refreshNewTokens(account, it) }
//    }
//  }

//  @CalledInAwt
//  @Throws(GiteeMissingTokenException::class)
//  fun getExecutor(account: GiteeAccount): GiteeApiRequestExecutor {
//    return authenticationManager.getTokensForAccount(account)?.let { it ->
//      requestExecutorFactory.create(it) { authenticationManager.refreshNewTokens(account, it) }
//    } ?: throw GiteeMissingTokenException(account)
//  }

  @CalledInAwt
  fun getExecutor(account: GiteeAccount, project: Project): GiteeApiRequestExecutor.WithTokensAuth? {
    return getOrTryToCreateExecutor(account) { authenticationManager.requestNewTokens(account, project) }
  }

  @CalledInAwt
  fun getExecutor(account: GiteeAccount, parentComponent: Component): GiteeApiRequestExecutor.WithTokensAuth? {
    return getOrTryToCreateExecutor(account) { authenticationManager.requestNewTokens(account, null, parentComponent) }
  }

  @CalledInAwt
  @Throws(GiteeMissingTokenException::class)
  fun getExecutor(account: GiteeAccount): GiteeApiRequestExecutor.WithTokensAuth {
    return getOrTryToCreateExecutor(account) { throw GiteeMissingTokenException(account) }!!
  }

//  private fun getOrTryToCreateExecutor(account: GiteeAccount,
//                                       missingTokenHandler: () -> String?): GiteeApiRequestExecutor.WithTokenAuth? {
//
//    return executors.getOrPut(account) {
//      (authenticationManager.getTokenForAccount(account) ?: missingTokenHandler())
//        ?.let(requestExecutorFactory::create) ?: return null
//    }
//  }

  private fun getOrTryToCreateExecutor(account: GiteeAccount,
                                       missingTokensHandler: () -> Pair<String, String>?): GiteeApiRequestExecutor.WithTokensAuth? {

    // 先从本地credential系统取, 如果没有 => missingTokensHandler弹窗请求输入
    // 如果本地credential取到, 创建附带刷新回调的executor
    return executors.getOrPut(account) {
      (authenticationManager.getTokensForAccount(account) ?: missingTokensHandler())
        ?.let { tokens ->
          requestExecutorFactory.create(tokens) { refreshToken ->
            authenticationManager.refreshNewTokens(account, refreshToken)
          }
        } ?: return null
    }
  }


  companion object {
    @JvmStatic
    fun getInstance(): GiteeApiRequestExecutorManager = service()
  }
}