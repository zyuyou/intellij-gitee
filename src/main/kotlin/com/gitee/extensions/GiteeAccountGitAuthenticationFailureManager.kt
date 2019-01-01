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
package com.gitee.extensions

import com.gitee.authentication.accounts.AccountTokenChangedListener
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.accounts.GiteeAccountManager
import com.intellij.openapi.application.ApplicationManager
import java.util.concurrent.ConcurrentHashMap

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/extensions/GithubAccountGitAuthenticationFailureManager.kt
 * @author JetBrains s.r.o.
 */
class GiteeAccountGitAuthenticationFailureManager {
  private val storeMap = ConcurrentHashMap<GiteeAccount, Set<String>>()

  init {
    ApplicationManager.getApplication().messageBus
      .connect()
      .subscribe(GiteeAccountManager.ACCOUNT_TOKEN_CHANGED_TOPIC, object : AccountTokenChangedListener {
        override fun tokenChanged(account: GiteeAccount) {
          storeMap.remove(account)
        }
      })
  }

  fun ignoreAccount(url: String, account: GiteeAccount) {
    storeMap.compute(account) { _, current -> current?.plus(url) ?: setOf(url) }
  }

  fun isAccountIgnored(url: String, account: GiteeAccount): Boolean = storeMap[account]?.contains(url) ?: false
}
