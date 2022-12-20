// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.ui

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GECredentials
import com.gitee.authentication.util.GiteeCredentialsCreator
import com.gitee.i18n.GiteeBundle.message
import com.gitee.ui.util.Validator
import com.intellij.openapi.progress.runUnderIndicator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.dsl.builder.Panel
import com.intellij.util.ui.UIUtil.getInactiveTextColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.JComponent

internal class GERefreshCredentialsUi(
  val factory: GiteeApiRequestExecutor.Factory,
  val isAccountUnique: UniqueLoginPredicate
) : GECredentialsUi() {

  private var fixedCredentials: GECredentials = GECredentials.EmptyCredentials

  override fun getPreferredFocusableComponent(): JComponent? = null

  override fun getValidator(): Validator = { null }

  override suspend fun login(server: GiteeServerPath): Pair<String, GECredentials> =
    withContext(Dispatchers.Main.immediate) {
      val executor = factory.create(fixedCredentials)

      val credentials = withContext(Dispatchers.IO) {
        runUnderIndicator {
          GiteeCredentialsCreator(server, executor).refresh(fixedCredentials.refreshToken)
        }
      }

      executor.credentials = credentials

      val login = GETokenCredentialsUi.acquireLogin(server, executor, isAccountUnique, null)

      login to credentials
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

  fun setFixedCredentials(credentials: GECredentials?) {
    if(credentials != null) {
      fixedCredentials = credentials
    }
  }
}