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
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.accounts.GiteeAccountInformationProvider
import com.gitee.authentication.ui.GiteeChooseAccountDialog
import com.gitee.util.GiteeUtil
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeAndWaitIfNeed
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.util.AuthData
import git4idea.DialogManager
import git4idea.remote.InteractiveGitHttpAuthDataProvider
import org.jetbrains.annotations.CalledInAwt
import java.awt.Component

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/extensions/InteractiveGiteeHttpAuthDataProvider.kt
 * @author JetBrains s.r.o.
 */
internal class InteractiveGiteeHttpAuthDataProvider(private val project: Project,
                                                    private val potentialAccounts: Collection<GiteeAccount>,
                                                    private val authenticationManager: GiteeAuthenticationManager) : InteractiveGitHttpAuthDataProvider {

  @CalledInAwt
  override fun getAuthData(parentComponent: Component?): AuthData? {
    val dialog = GiteeChooseAccountDialog(
      project,
      parentComponent,
      potentialAccounts,
      null,
      false,
      true,
      "Choose Gitee Account",
      "Log In"
    )

    DialogManager.show(dialog)
    if (!dialog.isOK) return null

    val account = dialog.account

//    val modalityStateSupplier = { parentComponent?.let(ModalityState::stateForComponent) ?: ModalityState.any() }

    val tokens = invokeAndWaitIfNeed(parentComponent?.let(ModalityState::stateForComponent) ?: ModalityState.any()) {
//      authenticationManager.getOrRequestTokensForAccount(account, project, parentComponent)
      authenticationManager.getOrRefreshTokensForAccount(account) ?: authenticationManager.requestNewTokens(account, project, parentComponent)
    } ?: return null

//    val tokens = authenticationManager.getOrRequestTokensForAccount(account,
//      parentComponent = parentComponent, modalityStateSupplier = modalityStateSupplier) ?: return null
    if (dialog.setDefault) authenticationManager.setDefaultAccount(project, account)

//    return AuthData(GiteeUtil.GIT_AUTH_PASSWORD_SUBSTITUTE, tokens.first)
    return AuthData(account.name, tokens.first)
  }
}