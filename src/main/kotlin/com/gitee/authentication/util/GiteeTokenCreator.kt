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
import com.gitee.api.data.GiteeAuthorization
import com.gitee.exceptions.GiteeStatusCodeException
import com.intellij.openapi.progress.ProgressIndicator
import java.io.IOException

/**
 * Handy helper for creating OAuth token
 *
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/authentication/util/GithubTokenCreator.kt
 * @author JetBrains s.r.o.
 */
class GiteeTokenCreator(private val server: GiteeServerPath,
                        private val executor: GiteeApiRequestExecutor,
                        private val indicator: ProgressIndicator) {

  @Throws(IOException::class)
  fun createMaster(login: String, password: CharArray): GiteeAuthorization {
    return safeCreate(MASTER_SCOPES, login, password)
  }

  @Throws(IOException::class)
  fun updateMaster(refreshToken: String): GiteeAuthorization {
    return safeUpdate(refreshToken)
  }

  @Throws(IOException::class)
  private fun safeCreate(scopes: List<String>, login: String, password: CharArray): GiteeAuthorization {
    try {
      return executor.execute(indicator, GiteeApiRequests.Auth.create(server, scopes, login, password))
    } catch (e: GiteeStatusCodeException) {
      e.setDetails("Can't create token: scopes - $scopes")
      throw e
    }
  }

  @Throws(IOException::class)
  private fun safeUpdate(refreshToken: String): GiteeAuthorization {
    try {
      return executor.execute(indicator, GiteeApiRequests.Auth.update(server, refreshToken))
    } catch (e: GiteeStatusCodeException) {
      throw e
    }
  }

  companion object {
    private val MASTER_SCOPES = listOf(
      "projects",
      "pull_requests",
      "gists",
      "issues",
      "user_info",
      "notes",
      "groups"
    )
//    const val DEFAULT_CLIENT_NAME = "Gitee Integration Plugin"
    const val DEFAULT_CLIENT_ID = "fc439d90cb2ffc20cffeb70a6a4039e69847485e0fa56cfa0d1bf006098e24dd"
    const val DEFAULT_CLIENT_SECRET = "386f187646ee361049f69cd213424bdba5af03e820d10a68a68e5fb520902596"
  }
}