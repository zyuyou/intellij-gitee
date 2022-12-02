// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.extensions

import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GEAccountAuthData
import com.gitee.authentication.GECredentials
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GiteeAccount
import com.intellij.openapi.project.Project
import com.intellij.util.AuthData
import com.intellij.util.concurrency.annotations.RequiresEdt
import git4idea.remote.InteractiveGitHttpAuthDataProvider
import java.awt.Component

//private val authenticationManager get() = GiteeAuthenticationManager.getInstance()
//
//internal class GECreateAccountHttpAuthDataProvider(
//  private val project: Project,
//  private val serverPath: GiteeServerPath,
//  private val login: String? = null
//) : InteractiveGitHttpAuthDataProvider {
//
//  @RequiresEdt
//  override fun getAuthData(parentComponent: Component?): AuthData? {
//    val account = requestNewAccount(parentComponent) ?: return null
//    val credentials = getOrRequestCredentials(account, project, parentComponent) ?: return null
//    return GEAccountAuthData(account, login ?: account.name, credentials)
//  }
//
//  private fun requestNewAccount(parentComponent: Component?): GiteeAccount? =
//    if (login != null)
//      authenticationManager.requestNewAccountForServer(serverPath, login, project, parentComponent)
//    else
//      authenticationManager.requestNewAccountForServer(serverPath, project, parentComponent)
//
//  companion object {
//    fun getOrRequestCredentials(account: GiteeAccount, project: Project, parentComponent: Component?): GECredentials? =
//      authenticationManager.getCredentialsForAccount(account)?.let {
//        // 如果已经过期, 重新刷新一下, 因为AuthData只附带了AccessToken
//        if (it.isAccessTokenValid())
//          return it
//
//        authenticationManager.requestUpdateCredentials(account, it, project, parentComponent)
//      } ?: authenticationManager.requestNewCredentials(account, project, parentComponent)
//  }
//}