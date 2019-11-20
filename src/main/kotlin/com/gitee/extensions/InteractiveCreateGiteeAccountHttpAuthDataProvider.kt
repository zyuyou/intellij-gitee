// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.extensions

import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GiteeAccount
import com.intellij.openapi.project.Project
import com.intellij.util.AuthData
import git4idea.remote.InteractiveGitHttpAuthDataProvider
import org.jetbrains.annotations.CalledInAwt
import java.awt.Component

internal class InteractiveCreateGiteeAccountHttpAuthDataProvider(private val project: Project,
                                                                 private val authenticationManager: GiteeAuthenticationManager,
                                                                 private val serverPath: GiteeServerPath,
                                                                 private val login: String? = null)
    : InteractiveGitHttpAuthDataProvider {

    @CalledInAwt
    override fun getAuthData(parentComponent: Component?): AuthData? {
        if (login == null) {
            val account = authenticationManager.requestNewAccountForServer(serverPath, project, parentComponent)
                    ?: return null

//      val token = getToken(account, parentComponent) ?: return null
//      return GiteeHttpAuthDataProvider.GiteeAccountAuthData(account, GiteeUtil.GIT_AUTH_PASSWORD_SUBSTITUTE, token)
            val tokens = getTokens(account, parentComponent) ?: return null
            return GiteeHttpAuthDataProvider.GiteeAccountAuthData(account, account.name, tokens.first)
        } else {
            val account = authenticationManager.requestNewAccountForServer(serverPath, login, project, parentComponent)
                    ?: return null

//            val token = getToken(account, parentComponent) ?: return null
//            return GiteeHttpAuthDataProvider.GiteeAccountAuthData(account, login, token)
          val tokens = getTokens(account, parentComponent) ?: return null
          return GiteeHttpAuthDataProvider.GiteeAccountAuthData(account, login, tokens.first)

        }
    }

//    private fun getToken(account: GiteeAccount, parentComponent: Component?) =
//            authenticationManager.getTokenForAccount(account)
//                    ?: authenticationManager.requestNewToken(account, project, parentComponent)

    private fun getTokens(account: GiteeAccount, parentComponent: Component?) =
            authenticationManager.getOrRefreshTokensForAccount(account)
                    ?: authenticationManager.requestNewTokens(account, project, parentComponent)
}