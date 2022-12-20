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
package com.gitee.authentication.util

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequests
import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GEAccountsUtil
import com.gitee.authentication.GECredentials
import com.gitee.exceptions.GiteeAuthenticationException
import com.gitee.exceptions.GiteeStatusCodeException
import com.intellij.openapi.progress.ProgressManager
import java.io.IOException

/**
 * Handy helper for creating OAuth token
 *
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/authentication/util/GithubTokenCreator.kt
 * @author JetBrains s.r.o.
 */
class GiteeCredentialsCreator(private val server: GiteeServerPath,
                              private val executor: GiteeApiRequestExecutor) {

  @Throws(IOException::class)
  fun create(login: String, password: CharArray): GECredentials {
    return safeCreate(login, password)
  }

  @Throws(IOException::class)
  fun refresh(refreshToken: String): GECredentials {
    return safeUpdate(refreshToken)
  }

  @Throws(IOException::class)
  private fun safeCreate(login: String, password: CharArray): GECredentials {
    try {
      val indicator = ProgressManager.getInstance().progressIndicator
      return executor.execute(
        indicator,
        GiteeApiRequests.Auth.create(server, GEAccountsUtil.APP_CLIENT_SCOPE, login, password)
      )
    } catch (e: GiteeStatusCodeException) {
      e.setDetails("Can't create token: scopes - ${GEAccountsUtil.APP_CLIENT_SCOPE}")
      throw e
    }
  }

  @Throws(IOException::class)
  private fun safeUpdate(refreshToken: String): GECredentials {
    try {
      val indicator = ProgressManager.getInstance().progressIndicator
      return executor.execute(indicator, GiteeApiRequests.Auth.update(server, refreshToken))
    } catch (e: GiteeStatusCodeException) {
      e.setDetails("Can't update token: scopes - ${GEAccountsUtil.APP_CLIENT_SCOPE}")
      throw e
    } catch (e: GiteeAuthenticationException) {
      throw e
    }
  }
}