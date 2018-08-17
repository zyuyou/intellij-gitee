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
package com.gitee.authentication.accounts

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequests
import com.gitee.api.data.GiteeUserDetailed
import com.google.common.cache.CacheBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.annotations.CalledInBackground
import java.awt.Image
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
class GiteeAccountInformationProvider {

  private val informationCache = CacheBuilder.newBuilder()
    .expireAfterAccess(30, TimeUnit.MINUTES)
    .build<GiteeAccount, GiteeUserDetailed>()
    .asMap()

  private val avatarCache = CacheBuilder.newBuilder()
    .expireAfterAccess(30, TimeUnit.MINUTES)
    .build<GiteeAccount, Image>()
    .asMap()

  init {
    ApplicationManager.getApplication().messageBus
      .connect()
      .subscribe(GiteeAccountManager.ACCOUNT_TOKEN_CHANGED_TOPIC, object : AccountTokenChangedListener {
        override fun tokenChanged(account: GiteeAccount) {
          informationCache.remove(account)
          avatarCache.remove(account)
        }
      })
  }

  @CalledInBackground
  @Throws(IOException::class)
  fun getInformation(executor: GiteeApiRequestExecutor, indicator: ProgressIndicator, account: GiteeAccount): GiteeUserDetailed {
    return informationCache.getOrPut(account) { executor.execute(indicator, GiteeApiRequests.CurrentUser.get(account.server)) }
  }

  @CalledInBackground
  @Throws(IOException::class)
  fun getAvatar(executor: GiteeApiRequestExecutor, indicator: ProgressIndicator, account: GiteeAccount, url: String): Image {
    return avatarCache.getOrPut(account) { executor.execute(indicator, GiteeApiRequests.CurrentUser.getAvatar(url)) }
  }
}