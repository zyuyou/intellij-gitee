// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.extensions

import com.gitee.authentication.accounts.GEAccountManager
import com.gitee.authentication.accounts.GiteeAccount
import com.intellij.collaboration.auth.AccountsListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import java.util.concurrent.ConcurrentHashMap

internal class GEGitAuthenticationFailureManager : Disposable {
  private val storeMap = ConcurrentHashMap<GiteeAccount, Set<String>>()

  init {
    service<GEAccountManager>().addListener(this, object : AccountsListener<GiteeAccount> {
      override fun onAccountCredentialsChanged(account: GiteeAccount) {
        storeMap.remove(account)
      }
    })
  }

  fun ignoreAccount(url: String, account: GiteeAccount) {
    storeMap.compute(account) { _, current -> current?.plus(url) ?: setOf(url) }
  }

  fun isAccountIgnored(url: String, account: GiteeAccount): Boolean = storeMap[account]?.contains(url) ?: false

  override fun dispose() {
    storeMap.clear()
  }
}