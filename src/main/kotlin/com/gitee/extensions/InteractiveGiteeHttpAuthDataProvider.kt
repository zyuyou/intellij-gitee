// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.extensions

import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.util.AuthData
import git4idea.DialogManager
import git4idea.remote.InteractiveGitHttpAuthDataProvider
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.ui.GiteeChooseAccountDialog
import com.gitee.util.GiteeUtil
import org.jetbrains.annotations.CalledInAwt
import javax.swing.JComponent

internal class InteractiveGiteeHttpAuthDataProvider(private val project: Project,
                                                    private val potentialAccounts: Collection<com.gitee.authentication.accounts.GiteeAccount>,
                                                    private val authenticationManager: GiteeAuthenticationManager) : InteractiveGitHttpAuthDataProvider {

  @CalledInAwt
  override fun getAuthData(parentComponent: JComponent?): AuthData? {
    val dialog = GiteeChooseAccountDialog(project,
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

    val modalityStateSupplier = { parentComponent?.let(ModalityState::stateForComponent) ?: ModalityState.any() }

    val tokens = authenticationManager.getOrRequestTokensForAccount(account,
      parentComponent = parentComponent, modalityStateSupplier = modalityStateSupplier) ?: return null

    if (dialog.setDefault) authenticationManager.setDefaultAccount(project, account)

    return AuthData(GiteeUtil.GIT_AUTH_PASSWORD_SUBSTITUTE, tokens.first)
  }
}