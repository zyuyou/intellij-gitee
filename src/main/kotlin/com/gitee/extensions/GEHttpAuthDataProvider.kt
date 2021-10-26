// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.gitee.extensions

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequestExecutorManager
import com.gitee.api.data.GiteeAuthenticatedUser
import com.gitee.authentication.GEAccountAuthData
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.accounts.GiteeAccountInformationProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.util.AuthData
import git4idea.remote.GitHttpAuthDataProvider

private val LOG = logger<GEHttpAuthDataProvider>()

internal class GEHttpAuthDataProvider : GitHttpAuthDataProvider {
  override fun isSilent(): Boolean = true

  override fun getAuthData(project: Project, url: String): GEAccountAuthData? {
    val account = getGitAuthenticationAccounts(project, url, null).singleOrNull() ?: return null
    val token = GiteeAuthenticationManager.getInstance().getTokenForAccount(account) ?: return null
    val accountDetails = getAccountDetails(account, token) ?: return null

    return GEAccountAuthData(account, accountDetails.login, token)
  }

  override fun getAuthData(project: Project, url: String, login: String): GEAccountAuthData? {
    val account = getGitAuthenticationAccounts(project, url, login).singleOrNull() ?: return null
    val token = GiteeAuthenticationManager.getInstance().getTokenForAccount(account) ?: return null

    return GEAccountAuthData(account, login, token)
  }

  override fun forgetPassword(project: Project, url: String, authData: AuthData) {
    if (authData !is GEAccountAuthData) return

    project.service<GEGitAuthenticationFailureManager>().ignoreAccount(url, authData.account)
  }

  companion object {
    fun getGitAuthenticationAccounts(project: Project, url: String, login: String?): Set<GiteeAccount> {
      val authenticationFailureManager = project.service<GEGitAuthenticationFailureManager>()
      val authenticationManager = GiteeAuthenticationManager.getInstance()
      val potentialAccounts = authenticationManager.getAccounts()
        .filter { it.server.matches(url) }
        .filterNot { authenticationFailureManager.isAccountIgnored(url, it) }
        .filter { login == null || login == getAccountDetails(it)?.login }

      val defaultAccount = authenticationManager.getDefaultAccount(project)
      if (defaultAccount != null && defaultAccount in potentialAccounts) return setOf(defaultAccount)
      return potentialAccounts.toSet()
    }
  }
}

private fun getAccountDetails(account: GiteeAccount, token: String? = null): GiteeAuthenticatedUser? =
  try {
    service<GiteeAccountInformationProvider>().getInformation(getRequestExecutor(account, token), DumbProgressIndicator(), account)
  }
  catch (e: Exception) {
    if (e !is ProcessCanceledException) LOG.info("Cannot load details for $account", e)
    null
  }

private fun getRequestExecutor(account: GiteeAccount, token: String?): GiteeApiRequestExecutor =
  if (token != null) GiteeApiRequestExecutor.Factory.getInstance().create(token)
  else GiteeApiRequestExecutorManager.getInstance().getExecutor(account)