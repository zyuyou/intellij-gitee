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

import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.util.GECompatibilityUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread

/**
 * Allows to acquire API executor without exposing the auth token to external code
 *
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/api/GiteeApiRequestExecutorManager.kt
 * @author JetBrains s.r.o.
 */
@Deprecated("Use com.gitee.api.GiteeApiRequestExecutor.Factory.Companion directly")
class GiteeApiRequestExecutorManager {
  companion object {
    @JvmStatic
    fun getInstance(): GiteeApiRequestExecutorManager = service()
  }

  @Deprecated("One-time use executor should not be persisted")
  @RequiresBackgroundThread
  fun getExecutor(account: GiteeAccount, project: Project): GiteeApiRequestExecutor? {
    val credentials = GECompatibilityUtil.getOrRequestCredentials(account, project) ?: return null
    return GiteeApiRequestExecutor.Factory.getInstance().create(credentials)
  }
}