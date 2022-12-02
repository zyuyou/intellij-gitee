// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.extensions

import com.gitee.api.GiteeServerPath.Companion.DEFAULT_SERVER
import com.gitee.authentication.GEAccountsUtil
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GEAccountManager
import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.util.AuthData
import git4idea.remote.GitHttpAuthDataProvider
import git4idea.remote.hosting.GitHostingUrlUtil.match
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

private class GEComHttpAuthDataProvider : GitHttpAuthDataProvider {
  override fun isSilent(): Boolean = false

  override fun getAuthData(project: Project, url: String, login: String): AuthData? {
    if (!match(DEFAULT_SERVER.toURI(), url)) return null

    return runBlocking { getAuthDataOrCancel(project, url, login) }
  }

  override fun getAuthData(project: Project, url: String): AuthData? {
    if (!match(DEFAULT_SERVER.toURI(), url)) return null

    return runBlocking { getAuthDataOrCancel(project, url, null) }
  }
}

private suspend fun getAuthDataOrCancel(project: Project, url: String, login: String?): AuthData {
//  val accounts = GiteeAuthenticationManager.getInstance().getAccounts().filter { match(it.server.toURI(), url) }
//  val provider = when (accounts.size) {
//    0 -> GECreateAccountHttpAuthDataProvider(project, DEFAULT_SERVER, login)
//    1 -> GEUpdateCredentialsHttpAuthDataProvider(project, accounts.first())
//    else -> GESelectAccountHttpAuthDataProvider(project, accounts)
//  }
//  val authData = invokeAndWaitIfNeeded(ModalityState.any()) { provider.getAuthData(null) }
//
//  return authData ?: throw ProcessCanceledException()
  val accountManager = service<GEAccountManager>()
  val accountsWithCredentials = accountManager.accountsState.value
    .filter { match(it.server.toURI(), url) }
    .associateWith { accountManager.findCredentials(it) }

  return withContext(Dispatchers.EDT + ModalityState.any().asContextElement()) {
    when (accountsWithCredentials.size) {
      0 -> GEAccountsUtil.requestNewAccount(DEFAULT_SERVER, login, project)
      1 -> GEAccountsUtil.requestReLogin(accountsWithCredentials.keys.first(), project = project)
      else -> GESelectAccountHttpAuthDataProvider(project, accountsWithCredentials).getAuthData(null)
    }
  } ?: throw ProcessCanceledException()
}