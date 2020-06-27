// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.ui

import com.gitee.GiteeBundle.Companion.message
import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeServerPath
import com.gitee.authentication.util.GiteeTokenCreator
import com.gitee.ui.util.DialogValidationUtils.notBlank
import com.gitee.util.completionOnEdt
import com.gitee.util.errorOnEdt
import com.gitee.util.submitIOTask
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.layout.LayoutBuilder
import java.util.concurrent.CompletableFuture
import javax.swing.JComponent
import javax.swing.JPasswordField
import javax.swing.JTextField

internal typealias UniqueLoginPredicate = (login: String, server: GiteeServerPath) -> Boolean

internal fun GiteeLoginPanel.setTokenUi() = setToken(null)
internal fun GiteeLoginPanel.setPasswordUi() = setCredentials(null, null, true)

class GiteeLoginPanel(
  executorFactory: GiteeApiRequestExecutor.Factory,
  isAccountUnique: UniqueLoginPredicate
) : Wrapper() {

  private val serverTextField = ExtendableTextField(GiteeServerPath.DEFAULT_HOST, 0)
  private var tokenAcquisitionError: ValidationInfo? = null

  private var clientIdTextField = JBTextField(GiteeTokenCreator.DEFAULT_CLIENT_ID, 5)
  private var clientSecretTextField = JPasswordField(GiteeTokenCreator.DEFAULT_CLIENT_SECRET, 5)

  private lateinit var currentUi: GECredentialsUi

//  private var passwordUi = GiteeCredentialsUI.PasswordUI(
//    serverTextField, clientIdTextField, clientSecretTextField, ::switchToTokenUI, ::editCustomAppInfo, ::useDefaultAppInfo, executorFactory, isAccountUnique, isDialogMode
//  )
  private var passwordUi = GEPasswordCredentialsUi(
    serverTextField, executorFactory, isAccountUnique, clientIdTextField, clientSecretTextField, ::editCustomAppInfo, ::useDefaultAppInfo
  )

//  private var tokenUi = GiteeCredentialsUI.TokenUI(
//    executorFactory, isAccountUnique, serverTextField, ::switchToPasswordUI, isDialogMode
//  )
  private var tokenUi = GETokenCredentialsUi(serverTextField, executorFactory, isAccountUnique)

  private var oauthUi = GEOAuthCredentialsUi(executorFactory, isAccountUnique)

  private val progressIcon = AnimatedIcon.Default()
  private val progressExtension = ExtendableTextComponent.Extension { progressIcon }

  var footer: LayoutBuilder.() -> Unit
    get() = tokenUi.footer
    set(value) {
      passwordUi.footer = value
      tokenUi.footer = value
      applyUi(currentUi)
    }

  init {
    applyUi(passwordUi)
  }

  private fun editCustomAppInfo() {
//    setContent(currentUi.getPanel2())
//    currentUi.getPreferredFocus().requestFocus()
  }

  private fun useDefaultAppInfo() {
//    setContent(currentUi.getPanel())
//    currentUi.getPreferredFocus().requestFocus()
  }

  private fun applyUi(ui: GECredentialsUi) {
    currentUi = ui
    setContent(currentUi.getPanel())
    currentUi.getPreferredFocusableComponent()?.requestFocus()
    tokenAcquisitionError = null
  }

  fun createSwitchUiLink(): LinkLabel<*> {
    fun switchUiText(): String = if (currentUi == passwordUi) message("login.use.token") else message("login.use.credentials")
    fun nextUi(): GECredentialsUi = if (currentUi == passwordUi) tokenUi else passwordUi

    return LinkLabel<Any?>(switchUiText(), null) { link, _ ->
      applyUi(nextUi())
      link.text = switchUiText()
    }
  }

  fun getPreferredFocusableComponent(): JComponent? =
    serverTextField.takeIf { it.isEditable && it.text.isBlank() }
      ?: currentUi.getPreferredFocusableComponent()

  fun doValidateAll(): List<ValidationInfo> {
    val uiError =
      notBlank(serverTextField, message("credentials.server.cannot.be.empty"))
        ?: validateServerPath(serverTextField)
        ?: currentUi.getValidator().invoke()

    return listOfNotNull(uiError, tokenAcquisitionError)
  }

  private fun validateServerPath(field: JTextField): ValidationInfo? =
    try {
      GiteeServerPath.from(field.text)
      null
    }
    catch (e: Exception) {
      ValidationInfo(message("credentials.server.path.invalid"), field)
    }

  private fun setBusy(busy: Boolean) {
    serverTextField.apply { if (busy) addExtension(progressExtension) else removeExtension(progressExtension) }
    serverTextField.isEnabled = !busy

    currentUi.setBusy(busy)
  }

  fun acquireLoginAndToken(progressIndicator: ProgressIndicator): CompletableFuture<Triple<String, String, String>> {
    setBusy(true)
    tokenAcquisitionError = null

    val server = getServer()
    val executor = currentUi.createExecutor()

    return service<ProgressManager>()
      .submitIOTask(progressIndicator) { currentUi.acquireLoginAndToken(server, executor, it) }
      .completionOnEdt(progressIndicator.modalityState) { setBusy(false) }
      .errorOnEdt(progressIndicator.modalityState) { setError(it) }
  }

  fun getServer(): GiteeServerPath =
    GiteeServerPath.from(serverTextField.text.trim(), clientIdTextField.text.trim(), String(clientSecretTextField.password))

  fun setServer(path: String, editable: Boolean = true) {
    serverTextField.apply {
      text = path
      isEditable = editable
    }

    clientIdTextField.isEditable = editable
    clientSecretTextField.isEditable = editable

    if (editable) {
      clientIdTextField.text = ""
      clientSecretTextField.text = ""
    }

//    if (GiteeServerPath.from(path).host == GiteeServerPath.DEFAULT_HOST) {
//      clientIdTextField.isEditable = false
//      clientSecretTextField.isEditable = false
//    }
  }

  fun setCredentials(login: String? = null, password: String? = null, editableLogin: Boolean = true) {
    if (login != null) {
      passwordUi.setLogin(login, editableLogin)
      tokenUi.setFixedLogin(if (editableLogin) null else login)
    }
    if (password != null) passwordUi.setPassword(password)
    applyUi(passwordUi)
  }

  fun setToken(token: String? = null) {
    if (token != null) tokenUi.setToken(token)
    applyUi(tokenUi)
  }

  fun setError(exception: Throwable) {
    tokenAcquisitionError = currentUi.handleAcquireError(exception)
  }

  fun setOAuthUi() = applyUi(oauthUi)

}