// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.ui

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GECredentials
import com.gitee.authentication.GEOAuthService
import com.gitee.i18n.GiteeBundle.message
import com.gitee.ui.util.Validator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.util.ProgressIndicatorUtils
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.dsl.builder.Panel
import com.intellij.util.ui.UIUtil.getInactiveTextColor
import javax.swing.JComponent

internal class GEOAuthCredentialsUi(
  val factory: GiteeApiRequestExecutor.Factory,
  val isAccountUnique: UniqueLoginPredicate
) : GECredentialsUi() {

  override fun getPreferredFocusableComponent(): JComponent? = null

  override fun getValidator(): Validator = { null }

  override fun createExecutor(): GiteeApiRequestExecutor = factory.create(GECredentials.EmptyCredentials)

  override fun acquireLoginAndToken(
    server: GiteeServerPath,
    executor: GiteeApiRequestExecutor,
    indicator: ProgressIndicator
  ): Pair<String, GECredentials> {
    executor as GiteeApiRequestExecutor.WithCredentialsAuth

    val credentials = acquireToken(indicator)
    executor.credentials = credentials

    val login = GETokenCredentialsUi.acquireLogin(server, executor, indicator, isAccountUnique, null)
    return Pair(login, credentials)
  }

  override fun handleAcquireError(error: Throwable): ValidationInfo = GETokenCredentialsUi.handleError(error)

  override fun setBusy(busy: Boolean) = Unit

  override fun Panel.centerPanel() {
    row {
      label(message("label.login.progress")).applyToComponent {
        icon = AnimatedIcon.Default()
        foreground = getInactiveTextColor()
      }
    }
  }

  private fun acquireToken(indicator: ProgressIndicator): GECredentials {
    val credentialsFuture = GEOAuthService.instance.authorize()

    try {
      return ProgressIndicatorUtils.awaitWithCheckCanceled(credentialsFuture, indicator)
    }
    catch (pce: ProcessCanceledException) {
      credentialsFuture.completeExceptionally(pce)
      throw pce
    }
  }
}