// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.ui

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GECredentials
import com.gitee.authentication.util.GiteeCredentialsCreator
import com.gitee.i18n.GiteeBundle.message
import com.gitee.ui.util.Validator
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBLabel
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.util.ui.UIUtil.getInactiveTextColor
import javax.swing.JComponent

internal class GERefreshCredentialsUi(
  val factory: GiteeApiRequestExecutor.Factory,
  val isAccountUnique: UniqueLoginPredicate
) : GECredentialsUi() {

  private val LOG = logger<GERefreshCredentialsUi>()

  private var fixedCredentials: GECredentials = GECredentials.EmptyCredentials

  override fun getPreferredFocusableComponent(): JComponent? = null

  override fun getValidator(): Validator = { null }

  override fun createExecutor(): GiteeApiRequestExecutor = factory.create(fixedCredentials)

  override fun acquireLoginAndToken(
    server: GiteeServerPath,
    executor: GiteeApiRequestExecutor,
    indicator: ProgressIndicator
  ): Pair<String, GECredentials> {
    LOG.info("fixedCredentials: ${fixedCredentials.accessToken}, ${fixedCredentials.refreshToken}, ${fixedCredentials.createdAt}")
    val credentials = GiteeCredentialsCreator(server, executor, indicator).refresh(fixedCredentials.refreshToken)

    LOG.info("credentials: ${credentials.accessToken}, ${credentials.refreshToken}, ${credentials.createdAt}")
    executor as GiteeApiRequestExecutor.WithCredentialsAuth
    executor.credentials = credentials

    val login = GETokenCredentialsUi.acquireLogin(server, executor, indicator, isAccountUnique, null)
    LOG.warn("login: $login")
    return Pair(login, credentials)
  }

  override fun handleAcquireError(error: Throwable): ValidationInfo = GETokenCredentialsUi.handleError(error)

  override fun setBusy(busy: Boolean) = Unit

  override fun LayoutBuilder.centerPanel() {
    row {
      val progressLabel = JBLabel(message("label.login.progress")).apply {
        icon = AnimatedIcon.Default()
        foreground = getInactiveTextColor()
      }
      progressLabel()
    }
  }

  fun setFixedCredentials(credentials: GECredentials?) {
    if(credentials != null) {
      fixedCredentials = credentials
    }
  }
}