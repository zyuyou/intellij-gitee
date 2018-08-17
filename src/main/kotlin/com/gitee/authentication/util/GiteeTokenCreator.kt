// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.util

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequests
import com.gitee.api.GiteeServerPath
import com.intellij.openapi.progress.ProgressIndicator
import java.io.IOException

/**
 * Handy helper for creating OAuth token
 */
class GiteeTokenCreator(private val server: GiteeServerPath,
                        private val executor: GiteeApiRequestExecutor,
                        private val indicator: ProgressIndicator) {

  @Throws(IOException::class)
  fun createMaster(login: String, password: CharArray): com.gitee.api.data.GiteeAuthorization {
    return safeCreate(MASTER_SCOPES, login, password)
  }

  @Throws(IOException::class)
  fun updateMaster(refreshToken: String): com.gitee.api.data.GiteeAuthorization {
    return safeUpdate(MASTER_SCOPES, refreshToken)
  }

  @Throws(IOException::class)
  private fun safeCreate(scopes: List<String>, login: String, password: CharArray): com.gitee.api.data.GiteeAuthorization {
    try {
      return executor.execute(indicator, GiteeApiRequests.Auth.create(server, scopes, login, password))
    }
    catch (e: com.gitee.exceptions.GiteeStatusCodeException) {
      e.setDetails("Can't create token: scopes - $scopes")
      throw e
    }
  }

  @Throws(IOException::class)
  private fun safeUpdate(scopes: List<String>, refreshToken: String): com.gitee.api.data.GiteeAuthorization {
    try {
      return executor.execute(indicator, GiteeApiRequests.Auth.update(server, refreshToken))
    }
    catch (e: com.gitee.exceptions.GiteeStatusCodeException) {
      e.setDetails("Can't create token: scopes - $scopes")
      throw e
    }
  }

  companion object {
    private val MASTER_SCOPES = listOf("projects", "pull_requests", "gists", "issues", "user_info")
//    const val DEFAULT_CLIENT_NAME = "Gitee Integration Plugin"
  }
}