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

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequestExecutorManager
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.accounts.GiteeAccountInformationProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.util.AuthData
import git4idea.remote.GitHttpAuthDataProvider
import java.io.IOException

private val LOG = logger<GiteeHttpAuthDataProvider>()

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/extensions/GithubHttpAuthDataProvider.kt
 * @author JetBrains s.r.o.
 */
class GiteeHttpAuthDataProvider : GitHttpAuthDataProvider {

  override fun getAuthData(project: Project, url: String): GiteeAccountAuthData? {
    return getSuitableAccounts(project, url, null).singleOrNull()?.let { account ->
      try {
        val tokens = GiteeAuthenticationManager.getInstance().getTokensForAccount(account) ?: return null

        val username = service<GiteeAccountInformationProvider>().getInformation(
          GiteeApiRequestExecutor.Factory.getInstance().create(tokens) {
//            GiteeAuthenticationManager.getInstance().refreshNewTokens(account, it)
            newTokens -> GiteeAuthenticationManager.getInstance().updateAccountToken(account, "${newTokens.first}&${newTokens.second}")
          },
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
      return GiteeAuthenticationManager.getInstance().getTokenForAccount(account)?.let { GiteeAccountAuthData(account, login, it) }
    }
  }

  override fun forgetPassword(project: Project, url: String, authData: AuthData) {
    if (authData is GiteeAccountAuthData) {
      project.service<GitAuthenticationFailureManager>().ignoreAccount(url, authData.account)
    }
  }

  fun getSuitableAccounts(project: Project, url: String, login: String?): Set<GiteeAccount> {

    val authenticationFailureManager = project.service<GitAuthenticationFailureManager>()

    var potentialAccounts = GiteeAuthenticationManager.getInstance().getAccounts()
      .filter { it.server.matches(url) }
      .filter { !authenticationFailureManager.isAccountIgnored(url, it) }

    if (login != null) {
      potentialAccounts = potentialAccounts.filter {
        try {
          service<GiteeAccountInformationProvider>().getInformation(
            GiteeApiRequestExecutorManager.getInstance().getExecutor(it),
            DumbProgressIndicator(),
            it).login == login

        } catch (e: IOException) {
          LOG.info("Cannot load username for $it", e)
          false
        }
      }
    }

    val defaultAccount = GiteeAuthenticationManager.getInstance().getDefaultAccount(project)

    if (defaultAccount != null && potentialAccounts.contains(defaultAccount)) return setOf(defaultAccount)

    return potentialAccounts.toSet()
  }

  class GiteeAccountAuthData(val account: GiteeAccount,
                             login: String,
                             password: String) : AuthData(login, password)
}