// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.accounts

import com.google.common.cache.CacheBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequests
import com.gitee.api.data.GiteeUserDetailed
import org.jetbrains.annotations.CalledInBackground
import java.awt.Image
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Loads the account information or provides it from cache
 * TODO: more abstraction
 */
class GiteeAccountInformationProvider {

  private val informationCache = CacheBuilder.newBuilder()
    .expireAfterAccess(30, TimeUnit.MINUTES)
    .build<com.gitee.authentication.accounts.GiteeAccount, com.gitee.api.data.GiteeUserDetailed>()
    .asMap()

  private val avatarCache = CacheBuilder.newBuilder()
    .expireAfterAccess(30, TimeUnit.MINUTES)
    .build<com.gitee.authentication.accounts.GiteeAccount, Image>()
    .asMap()

  init {
    ApplicationManager.getApplication().messageBus
      .connect()
      .subscribe(GiteeAccountManager.ACCOUNT_TOKEN_CHANGED_TOPIC, object : AccountTokenChangedListener {
        override fun tokenChanged(account: com.gitee.authentication.accounts.GiteeAccount) {
          informationCache.remove(account)
          avatarCache.remove(account)
        }
      })
  }

  @CalledInBackground
  @Throws(IOException::class)
  fun getInformation(executor: GiteeApiRequestExecutor, indicator: ProgressIndicator, account: com.gitee.authentication.accounts.GiteeAccount): com.gitee.api.data.GiteeUserDetailed {
    return informationCache.getOrPut(account) { executor.execute(indicator, GiteeApiRequests.CurrentUser.get(account.server)) }
  }

  @CalledInBackground
  @Throws(IOException::class)
  fun getAvatar(executor: GiteeApiRequestExecutor, indicator: ProgressIndicator, account: com.gitee.authentication.accounts.GiteeAccount, url: String): Image {
    return avatarCache.getOrPut(account) { executor.execute(indicator, GiteeApiRequests.CurrentUser.getAvatar(url)) }
  }
}