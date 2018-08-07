// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.gitee.extensions

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.util.AuthData
import git4idea.remote.GitHttpAuthDataProvider
import org.intellij.gitee.api.GiteeApiRequestExecutor
import org.intellij.gitee.api.GiteeApiRequestExecutorManager
import org.intellij.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GiteeAccount
import org.intellij.gitee.authentication.accounts.GiteeAccountInformationProvider
import java.io.IOException

class GiteeHttpAuthDataProvider(private val authenticationManager: GiteeAuthenticationManager,
                                private val requestExecutorFactory: GiteeApiRequestExecutor.Factory,
                                private val requestExecutorManager: GiteeApiRequestExecutorManager,
                                private val accountInformationProvider: GiteeAccountInformationProvider,
                                private val authenticationFailureManager: GiteeAccountGitAuthenticationFailureManager) : GitHttpAuthDataProvider {

  private val LOG = logger<GiteeHttpAuthDataProvider>()

  override fun getAuthData(project: Project, url: String): GiteeAccountAuthData? {
    return getSuitableAccounts(project, url, null).singleOrNull()?.let { account ->
      try {
        val tokens = authenticationManager.getTokensForAccount(account) ?: return null

        val username = accountInformationProvider.getInformation(
          requestExecutorFactory.create(tokens) { authenticationManager.refreshNewTokens(account, it) },
          DumbProgressIndicator(),
          account
        ).login

        GiteeAccountAuthData(account, username, tokens.first)
      } catch (e: IOException) {
        LOG.info("Cannot load username for $account", e)
        null
      }
    }
  }

  override fun getAuthData(project: Project, url: String, login: String): GiteeAccountAuthData? {
    return getSuitableAccounts(project, url, login).singleOrNull()?.let { account ->
      return authenticationManager.getTokenForAccount(account)?.let { GiteeAccountAuthData(account, login, it) }
    }
  }

  override fun forgetPassword(url: String, authData: AuthData) {
    if (authData is GiteeAccountAuthData) {
      authenticationFailureManager.ignoreAccount(url, authData.account)
    }
  }

  fun getSuitableAccounts(project: Project, url: String, login: String?): Set<com.gitee.authentication.accounts.GiteeAccount> {

    var potentialAccounts = authenticationManager.getAccounts()
      .filter { it.server.matches(url) }
      .filter { !authenticationFailureManager.isAccountIgnored(url, it) }

    if (login != null) {
      potentialAccounts = potentialAccounts.filter {
        try {
          accountInformationProvider.getInformation(
            requestExecutorManager.getExecutor(it),
            DumbProgressIndicator(),
            it).login == login

        } catch (e: IOException) {
          LOG.info("Cannot load username for $it", e)
          false
        }
      }
    }

    val defaultAccount = authenticationManager.getDefaultAccount(project)

    if (defaultAccount != null && potentialAccounts.contains(defaultAccount)) return setOf(defaultAccount)

    return potentialAccounts.toSet()
  }

  class GiteeAccountAuthData(val account: com.gitee.authentication.accounts.GiteeAccount,
                             login: String,
                             password: String) : AuthData(login, password)
}