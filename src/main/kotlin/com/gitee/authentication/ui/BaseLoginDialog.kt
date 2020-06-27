// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.ui

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeServerPath
import com.gitee.util.GiteeAsyncUtil
import com.gitee.util.completionOnEdt
import com.gitee.util.errorOnEdt
import com.gitee.util.successOnEdt
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DialogWrapper.IS_VISUAL_PADDING_COMPENSATED_ON_COMPONENT_LEVEL_KEY
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import java.awt.Component
import javax.swing.JComponent

internal fun JComponent.setPaddingCompensated(): JComponent =
  apply { putClientProperty(IS_VISUAL_PADDING_COMPENSATED_ON_COMPONENT_LEVEL_KEY, false) }

internal abstract class BaseLoginDialog(
  project: Project?,
  parent: Component?,
  executorFactory: GiteeApiRequestExecutor.Factory,
  isAccountUnique: UniqueLoginPredicate
) : DialogWrapper(project, parent, false, IdeModalityType.PROJECT) {

  protected val loginPanel = GiteeLoginPanel(executorFactory, isAccountUnique)

  private var _login = ""
  private var _token = ""
  private var _accessToken = ""
  private var _refreshToken = ""

  val login: String get() = _login
  val token: String get() = _token
  val accessToken: String get() = _accessToken
  val refreshToken: String get() = _refreshToken

  val server: GiteeServerPath get() = loginPanel.getServer()
  fun setServer(path: String, editable: Boolean) = loginPanel.setServer(path, editable)

  override fun getPreferredFocusedComponent(): JComponent? = loginPanel.getPreferredFocusableComponent()

  override fun doValidateAll(): List<ValidationInfo> = loginPanel.doValidateAll()

  override fun doOKAction() {
    val modalityState = ModalityState.stateForComponent(loginPanel)
    val emptyProgressIndicator = EmptyProgressIndicator(modalityState)
    Disposer.register(disposable, Disposable { emptyProgressIndicator.cancel() })

    startGettingToken()
    loginPanel.acquireLoginAndToken(emptyProgressIndicator)
      .completionOnEdt(modalityState) { finishGettingToken() }
      .successOnEdt(modalityState) { (login, accessToken, refreshToken) ->
        _login = login
        _token = "${accessToken}&${refreshToken}"
        _accessToken = accessToken
        _refreshToken = refreshToken

        close(OK_EXIT_CODE, true)
      }
      .errorOnEdt(modalityState) {
        if (!GiteeAsyncUtil.isCancellation(it)) startTrackingValidation()
      }
  }

  protected open fun startGettingToken() = Unit
  protected open fun finishGettingToken() = Unit
}