// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.extensions

import com.gitee.authentication.GEAccountAuthData
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.extensions.GECreateAccountHttpAuthDataProvider.Companion.getOrRequestCredentials
import com.intellij.openapi.project.Project
import com.intellij.util.AuthData
import com.intellij.util.concurrency.annotations.RequiresEdt
import git4idea.remote.InteractiveGitHttpAuthDataProvider
import java.awt.Component

private val authenticationManager get() = GiteeAuthenticationManager.getInstance()

internal class GEUpdateCredentialsHttpAuthDataProvider(
  private val project: Project,
  private val account: GiteeAccount
) : InteractiveGitHttpAuthDataProvider {

  @RequiresEdt
  override fun getAuthData(parentComponent: Component?): AuthData? {
    if (!authenticationManager.requestReLogin(account, project, parentComponent)) return null
    val credentials = getOrRequestCredentials(account, project, parentComponent) ?: return null

    return GEAccountAuthData(account, account.name, credentials)
  }
}