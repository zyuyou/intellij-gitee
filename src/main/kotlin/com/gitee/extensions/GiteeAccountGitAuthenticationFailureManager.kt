// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.extensions

import com.intellij.openapi.application.ApplicationManager
import org.intellij.gitee.authentication.accounts.AccountTokenChangedListener
import com.gitee.authentication.accounts.GiteeAccount
import org.intellij.gitee.authentication.accounts.GiteeAccountManager
import java.util.concurrent.ConcurrentHashMap

class GiteeAccountGitAuthenticationFailureManager {
  private val storeMap = ConcurrentHashMap<com.gitee.authentication.accounts.GiteeAccount, Set<String>>()

  init {
    ApplicationManager.getApplication().messageBus
      .connect()
      .subscribe(GiteeAccountManager.ACCOUNT_TOKEN_CHANGED_TOPIC, object : AccountTokenChangedListener {
        override fun tokenChanged(account: com.gitee.authentication.accounts.GiteeAccount) {
          storeMap.remove(account)
        }
      })
  }

  fun ignoreAccount(url: String, account: com.gitee.authentication.accounts.GiteeAccount) {
    storeMap.compute(account) { _, current -> current?.plus(url) ?: setOf(url) }
  }

  fun isAccountIgnored(url: String, account: com.gitee.authentication.accounts.GiteeAccount): Boolean = storeMap[account]?.contains(url) ?: false
}
