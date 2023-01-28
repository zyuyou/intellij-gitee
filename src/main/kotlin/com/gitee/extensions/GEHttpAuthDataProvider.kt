/*
 *  Copyright 2016-2022 码云 - Gitee
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

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.data.GiteeAuthenticatedUser
import com.gitee.authentication.GEAccountAuthData
import com.gitee.authentication.GECredentials
import com.gitee.authentication.accounts.GEAccountManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.accounts.GiteeAccountInformationProvider
import com.gitee.authentication.accounts.GiteeProjectDefaultAccountHolder
import com.gitee.exceptions.GiteeAccessTokenExpiredException
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.util.AuthData
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import git4idea.remote.GitHttpAuthDataProvider
import git4idea.remote.hosting.GitHostingUrlUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private val LOG = logger<GEHttpAuthDataProvider>()

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/extensions/GithubHttpAuthDataProvider.kt
 * @author JetBrains s.r.o.
 */
internal class GEHttpAuthDataProvider : GitHttpAuthDataProvider {
  override fun isSilent(): Boolean = true

  @RequiresBackgroundThread
  override fun getAuthData(project: Project, url: String): GEAccountAuthData? = runBlocking {
    doGetAuthData(project, url)
  }

  private suspend fun doGetAuthData(project: Project, url: String): GEAccountAuthData? {
    val defaultAuthData = getDefaultAccountData(project, url)
    if (defaultAuthData != null) {
      return defaultAuthData
    }

    return getAccountsWithTokens(project, url).entries
      .singleOrNull { it.value != null }?.let { (acc, credentials) ->
        val login = getAccountDetails(acc, credentials!!)?.login ?: return null
        GEAccountAuthData(acc, login, credentials)
      }
  }

  @RequiresBackgroundThread
  override fun getAuthData(project: Project, url: String, login: String): GEAccountAuthData? = runBlocking {
    doGetAuthData(project, url, login)
  }

  private suspend fun doGetAuthData(project: Project, url: String, login: String): GEAccountAuthData? {
    val defaultAuthData = getDefaultAccountData(project, url)
    if (defaultAuthData != null && defaultAuthData.login == login) {
      return defaultAuthData
    }

    return getAccountsWithTokens(project, url).mapNotNull { (acc, credentials) ->
      if (credentials == null) return@mapNotNull null
      val details = getAccountDetails(acc, credentials) ?: return@mapNotNull null
      if (details.login != login) return@mapNotNull null
      GEAccountAuthData(acc, login, credentials)
    }.singleOrNull()
  }

  override fun forgetPassword(project: Project, url: String, authData: AuthData) {
    if (authData !is GEAccountAuthData) return
    project.service<GEGitAuthenticationFailureManager>().ignoreAccount(url, authData.account)
  }

  companion object {
    private suspend fun getDefaultAccountData(project: Project, url: String): GEAccountAuthData? {
      val defaultAccount = project.service<GiteeProjectDefaultAccountHolder>().account ?: return null
      val authFailureManager = project.service<GEGitAuthenticationFailureManager>()

      if (GitHostingUrlUtil.match(defaultAccount.server.toURI(), url) && !authFailureManager.isAccountIgnored(
          url,
          defaultAccount
        )
      ) {
        val credentials = service<GEAccountManager>().findCredentials(defaultAccount) ?: return null
        val login = getAccountDetails(defaultAccount, credentials)?.login ?: return null
        return GEAccountAuthData(defaultAccount, login, credentials)
      }
      return null
    }

    suspend fun getAccountsWithTokens(project: Project, url: String): Map<GiteeAccount, GECredentials?> {
      val accountManager = service<GEAccountManager>()
      val authFailureManager = project.service<GEGitAuthenticationFailureManager>()

      return accountManager.accountsState.value
        .filter { GitHostingUrlUtil.match(it.server.toURI(), url) }
        .filterNot { authFailureManager.isAccountIgnored(url, it) }
        .associateWith { accountManager.findCredentials(it) }
    }

    suspend fun getAccountDetails(account: GiteeAccount, credentials: GECredentials): GiteeAuthenticatedUser? =
      try {
        if (!credentials.isAccessTokenValid())
          throw GiteeAccessTokenExpiredException("Account: ${account}'s credentials expire time: ${credentials.createdAt + credentials.expiresIn}")

        val executor = GiteeApiRequestExecutor.Factory.getInstance().create(credentials)
        withContext(Dispatchers.IO) {
          service<GiteeAccountInformationProvider>().getInformation(executor, DumbProgressIndicator(), account)
        }
      } catch (e: Exception) {
        if (e !is ProcessCanceledException) LOG.info("Cannot load details for $account", e)
        null
      }
  }
}