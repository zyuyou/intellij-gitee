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
package com.gitee.authentication.accounts

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequests
import com.gitee.api.data.GiteeAuthenticatedUser
import com.google.common.cache.CacheBuilder
import com.intellij.collaboration.async.disposingScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.childScope
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Loads the account information or provides it from cache
 * TODO: more abstraction
 *
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/authentication/accounts/GithubAccountInformationProvider.kt
 * @author JetBrains s.r.o.
 */
@Service(Service.Level.APP)
class GiteeAccountInformationProvider(cs: CoroutineScope) : Disposable {

  private val informationCache = CacheBuilder.newBuilder()
    .expireAfterAccess(30, TimeUnit.MINUTES)
    .build<GiteeAccount, GiteeAuthenticatedUser>()

  init {
    cs.childScope().launch {
      service<GEAccountManager>().accountsState.collect {
        informationCache.invalidateAll()
      }
    }
  }

  @RequiresBackgroundThread
  @Throws(IOException::class)
  fun getInformation(executor: GiteeApiRequestExecutor, indicator: ProgressIndicator, account: GiteeAccount): GiteeAuthenticatedUser {
    return informationCache.get(account) { executor.execute(indicator, GiteeApiRequests.CurrentUser.get(account.server)) }
  }

  companion object {
    @JvmStatic
    fun getInstance(): GiteeAccountInformationProvider {
      return service()
    }
  }

  override fun dispose() {
    informationCache.invalidateAll()
  }
}