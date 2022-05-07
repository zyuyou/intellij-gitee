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

import com.gitee.authentication.GECredentials
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GEAccountManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.exceptions.GiteeMissingTokenException
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresEdt
import java.awt.Component

/**
 * Allows to acquire API executor without exposing the auth token to external code
 *
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/api/GiteeApiRequestExecutorManager.kt
 * @author JetBrains s.r.o.
 */
class GiteeApiRequestExecutorManager {

  private val executors = mutableMapOf<GiteeAccount, GiteeApiRequestExecutor.WithCreateOrUpdateCredentialsAuth>()

  companion object {
    @JvmStatic
    fun getInstance(): GiteeApiRequestExecutorManager = service()
  }

  internal fun tokenChanged(account: GiteeAccount) {
    val credentials = service<GEAccountManager>().findCredentials(account)
    if (credentials == null)
      executors.remove(account)
    else
      executors[account]?.credentials = credentials
  }

  @RequiresEdt
  fun getExecutor(account: GiteeAccount, project: Project): GiteeApiRequestExecutor.WithCreateOrUpdateCredentialsAuth? {
    return getOrTryToCreateExecutor(account) {
      GiteeAuthenticationManager.getInstance().requestNewCredentials(account, project)
    }
  }

  @RequiresEdt
  fun getExecutor(account: GiteeAccount, parentComponent: Component): GiteeApiRequestExecutor.WithCreateOrUpdateCredentialsAuth? {
    return getOrTryToCreateExecutor(account) {
      GiteeAuthenticationManager.getInstance().requestNewCredentials(account, null, parentComponent)
    }
  }

  @RequiresEdt
  @Throws(GiteeMissingTokenException::class)
  fun getExecutor(account: GiteeAccount): GiteeApiRequestExecutor.WithCreateOrUpdateCredentialsAuth {
    return getOrTryToCreateExecutor(account) { throw GiteeMissingTokenException(account) }!!
  }

  private fun getOrTryToCreateExecutor(account: GiteeAccount,
                                       missingTokensHandler: () -> GECredentials?): GiteeApiRequestExecutor.WithCreateOrUpdateCredentialsAuth? {

    // 先从本地credential系统取, 如果没有 => missingTokensHandler弹窗请求输入
    // 如果本地credential取到, 创建附带刷新回调的executor
    return executors.getOrPut(account) {
      (GiteeAuthenticationManager.getInstance().getCredentialsForAccount(account) ?: missingTokensHandler())
        ?.let { credentials ->
          GiteeApiRequestExecutor.Factory.getInstance().create(credentials) { newCredentials ->
            GiteeAuthenticationManager.getInstance().updateAccountCredentials(account, newCredentials)
          }
        } ?: return null
    }
  }
}