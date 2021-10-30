// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.extensions

import com.gitee.api.GiteeServerPath.Companion.DEFAULT_SERVER
import com.gitee.authentication.GiteeAuthenticationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.util.AuthData
import git4idea.remote.GitHttpAuthDataProvider

private class GEComHttpAuthDataProvider : GitHttpAuthDataProvider {
  override fun isSilent(): Boolean = false

  override fun getAuthData(project: Project, url: String, login: String): AuthData? {
    if (!DEFAULT_SERVER.matches(url)) return null

    return getAuthDataOrCancel(project, url, login)
  }

  override fun getAuthData(project: Project, url: String): AuthData? {
    if (!DEFAULT_SERVER.matches(url)) return null

    return getAuthDataOrCancel(project, url, null)
  }
}

private fun getAuthDataOrCancel(project: Project, url: String, login: String?): AuthData {
  val accounts = GiteeAuthenticationManager.getInstance().getAccounts().filter { it.server.matches(url) }
  val provider = when (accounts.size) {
    0 -> GECreateAccountHttpAuthDataProvider(project, DEFAULT_SERVER, login)
    1 -> GEUpdateTokenHttpAuthDataProvider(project, accounts.first())
    else -> GESelectAccountHttpAuthDataProvider(project, accounts)
  }
  val authData = invokeAndWaitIfNeeded(ModalityState.any()) { provider.getAuthData(null) }

  return authData ?: throw ProcessCanceledException()
}