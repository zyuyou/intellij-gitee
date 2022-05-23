// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication

import com.gitee.api.GiteeServerPath
import com.gitee.authentication.accounts.GEAccountManager.Companion.createAccount
import com.gitee.authentication.ui.*
import com.gitee.i18n.GiteeBundle.message
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.NlsContexts
import git4idea.DialogManager
import java.awt.Component

internal class GELoginRequest(
  @NlsContexts.DialogMessage
  val text: String? = null,
  val error: Throwable? = null,

  val server: GiteeServerPath? = null,
  val isServerEditable: Boolean = server == null,

  val login: String? = null,
  val isLoginEditable: Boolean = true,
  val isCheckLoginUnique: Boolean = false,

  val token: String? = null,
  val credentials: GECredentials? = null
)

internal fun GELoginRequest.loginWithTokens(project: Project?, parentComponent: Component?): GEAccountAuthData? {
  val dialog = GETokensLoginDialog(project, parentComponent, isLoginUniqueChecker)
  configure(dialog)

  return dialog.getAuthData()
}

internal fun GELoginRequest.loginWithOAuth(project: Project?, parentComponent: Component?): GEAccountAuthData? {
  val dialog = GEOAuthLoginDialog(project, parentComponent, isLoginUniqueChecker)
  configure(dialog)

  return dialog.getAuthData()
}

internal fun GELoginRequest.loginWithPassword(project: Project?, parentComponent: Component?): GEAccountAuthData? {
  val dialog = GEPasswordLoginDialog(project, parentComponent, isLoginUniqueChecker)
  configure(dialog)

  return dialog.getAuthData()
}

internal fun GELoginRequest.loginRefreshTokens(project: Project?, parentComponent: Component?): GEAccountAuthData? {
  val dialog = GERefreshTokensLoginDialog(project, parentComponent, isLoginUniqueChecker)
  configure(dialog)

  return dialog.getAuthData()
}

internal fun GELoginRequest.loginWithOAuthOrTokens(project: Project?, parentComponent: Component?): GEAccountAuthData? =
  when (promptOAuthLogin(this, project, parentComponent)) {
    Messages.YES ->
      loginWithOAuth(project, parentComponent)
    Messages.NO -> {
      when (promptTokenLogin(this, project, parentComponent)) {
        Messages.YES ->
          loginWithPassword(project, parentComponent)
        Messages.NO ->
          loginWithTokens(project, parentComponent)
        else ->
          null
      }
    }
    else ->
      null
  }

private val GELoginRequest.isLoginUniqueChecker: UniqueLoginPredicate
  get() = { login, server -> !isCheckLoginUnique || GiteeAuthenticationManager.getInstance().isAccountUnique(login, server) }

private fun GELoginRequest.configure(dialog: BaseLoginDialog) {
  error?.let { dialog.setError(it) }
  server?.let { dialog.setServer(it.toString(), isServerEditable) }
  login?.let { dialog.setLogin(it, isLoginEditable) }
//  token?.let { dialog.setToken(it) }
  credentials?.let { dialog.setCredentials(it) }
}

private fun BaseLoginDialog.getAuthData(): GEAccountAuthData? {
  DialogManager.show(this)
  return if (isOK) GEAccountAuthData(createAccount(login, server), login, credentials) else null
}

private fun promptOAuthLogin(request: GELoginRequest, project: Project?, parentComponent: Component?): Int {
  val builder = MessageDialogBuilder.yesNoCancel(message("login.to.gitee"), request.text
    ?: message("dialog.message.login.to.continue"))
    .yesText(message("login.via.gitee.action"))
    .noText(message("button.use.other"))
    .icon(Messages.getWarningIcon())

  if (parentComponent != null) {
    return builder.show(parentComponent)
  } else {
    return builder.show(project)
  }
}

private fun promptTokenLogin(request: GELoginRequest, project: Project?, parentComponent: Component?): Int {
  val builder = MessageDialogBuilder.yesNoCancel(message("login.to.gitee"), request.text?: message("dialog.message.login.to.continue"))
    .yesText(message("button.use.password"))
    .noText(message("button.use.tokens"))
    .icon(Messages.getWarningIcon())

  if (parentComponent != null) {
    return builder.show(parentComponent)
  } else {
    return builder.show(project)
  }
}