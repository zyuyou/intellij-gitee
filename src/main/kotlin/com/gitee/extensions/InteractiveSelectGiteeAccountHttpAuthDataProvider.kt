// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.extensions

import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.ui.GiteeChooseAccountDialog
import com.intellij.openapi.project.Project
import com.intellij.util.AuthData
import git4idea.DialogManager
import git4idea.remote.InteractiveGitHttpAuthDataProvider
import org.jetbrains.annotations.CalledInAwt
import java.awt.Component

internal class InteractiveSelectGiteeAccountHttpAuthDataProvider(private val project: Project,
                                                                 private val potentialAccounts: Collection<GiteeAccount>,
                                                                 private val authenticationManager: GiteeAuthenticationManager) : InteractiveGitHttpAuthDataProvider {

    @CalledInAwt
    override fun getAuthData(parentComponent: Component?): AuthData? {
        val dialog = GiteeChooseAccountDialog(project, parentComponent, potentialAccounts,
                null, false, true, "Choose Gitee Account", "Log In")

        DialogManager.show(dialog)
        if (!dialog.isOK) return null
        val account = dialog.account

//        val token = authenticationManager.getTokenForAccount(account)
//                ?: authenticationManager.requestNewToken(account, project, parentComponent)
//                ?: return null

        val tokens = authenticationManager.getTokensForAccount(account)
                ?: authenticationManager.requestNewTokens(account, project, parentComponent)
                ?: return null

        if (dialog.setDefault) authenticationManager.setDefaultAccount(project, account)
        return GiteeHttpAuthDataProvider.GiteeAccountAuthData(account, account.name, tokens.first)
    }
}