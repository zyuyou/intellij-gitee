// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.ui

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GECredentials
import com.intellij.collaboration.async.CompletableFutureUtil
import com.intellij.collaboration.async.CompletableFutureUtil.completionOnEdt
import com.intellij.collaboration.async.CompletableFutureUtil.errorOnEdt
import com.intellij.collaboration.async.CompletableFutureUtil.successOnEdt
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
  private var _credentials = GECredentials.EmptyCredentials

  val login: String get() = _login
  val credentials: GECredentials get() = _credentials

  val server: GiteeServerPath get() = loginPanel.getServer()

  fun setLogin(login: String?, editable: Boolean) = loginPanel.setLogin(login, editable)

  fun setCredentials(credentials: GECredentials?) = loginPanel.setCredentials(credentials)

  fun setServer(path: String, editable: Boolean) = loginPanel.setServer(path, editable)

  fun setError(exception: Throwable) {
    loginPanel.setError(exception)
    startTrackingValidation()
  }

  override fun getPreferredFocusedComponent(): JComponent? = loginPanel.getPreferredFocusableComponent()

  override fun doValidateAll(): List<ValidationInfo> = loginPanel.doValidateAll()

  override fun doOKAction() {
    val modalityState = ModalityState.stateForComponent(loginPanel)
    val emptyProgressIndicator = EmptyProgressIndicator(modalityState)
    Disposer.register(disposable) { emptyProgressIndicator.cancel() }

    startGettingToken()

    loginPanel.acquireLoginAndToken(emptyProgressIndicator)
      .completionOnEdt(modalityState) { finishGettingToken() }
      .successOnEdt(modalityState) { (login, credentials) ->

        _login = login
        _credentials = credentials

        close(OK_EXIT_CODE, true)
      }
      .errorOnEdt(modalityState) {
        if (!CompletableFutureUtil.isCancellation(it)) startTrackingValidation()
      }
  }

  protected open fun startGettingToken() = Unit
  protected open fun finishGettingToken() = Unit
}