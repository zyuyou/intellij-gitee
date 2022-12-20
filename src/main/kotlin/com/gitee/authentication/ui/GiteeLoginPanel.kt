// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.ui

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GEAccountsUtil
import com.gitee.authentication.GECredentials
import com.gitee.i18n.GiteeBundle.message
import com.gitee.ui.util.DialogValidationUtils.notBlank
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.components.panels.Wrapper
import com.intellij.ui.dsl.builder.Panel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.swing.JComponent
import javax.swing.JPasswordField
import javax.swing.JTextField

internal typealias UniqueLoginPredicate = (login: String, server: GiteeServerPath) -> Boolean

class GiteeLoginPanel(
  executorFactory: GiteeApiRequestExecutor.Factory,
  isAccountUnique: UniqueLoginPredicate
) : Wrapper() {

  private val serverTextField = ExtendableTextField(GiteeServerPath.DEFAULT_HOST, 0)
  private var tokenAcquisitionError: ValidationInfo? = null

  private var clientIdTextField = JBTextField(GEAccountsUtil.APP_CLIENT_ID, 5)
  private var clientSecretTextField = JPasswordField(GEAccountsUtil.APP_CLIENT_SECRET, 5)

  private lateinit var currentUi: GECredentialsUi

  private var passwordUi = GEPasswordCredentialsUi(
    serverTextField, executorFactory, isAccountUnique, clientIdTextField, clientSecretTextField
  )

  private var tokenUi = GETokenCredentialsUi(serverTextField, executorFactory, isAccountUnique)

  private var oauthUi = GEOAuthCredentialsUi(executorFactory, isAccountUnique)

  private var refreshTokenUi = GERefreshCredentialsUi(executorFactory, isAccountUnique)

  private val progressIcon = AnimatedIcon.Default()
  private val progressExtension = ExtendableTextComponent.Extension { progressIcon }

  var footer: Panel.() -> Unit
    get() = tokenUi.footer
    set(value) {
      oauthUi.footer = value
      passwordUi.footer = value
      tokenUi.footer = value
      applyUi(currentUi)
    }

  init {
    applyUi(passwordUi)
  }

  private fun applyUi(ui: GECredentialsUi) {
    currentUi = ui
    setContent(currentUi.getPanel())
    currentUi.getPreferredFocusableComponent()?.requestFocus()
    tokenAcquisitionError = null
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

//  fun acquireLoginAndToken(progressIndicator: ProgressIndicator): CompletableFuture<Pair<String, GECredentials>> {
//    setBusy(true)
//    tokenAcquisitionError = null
//
//    val server = getServer()
//    val executor = currentUi.createExecutor()
//
//    return service<ProgressManager>()
//      .submitIOTask(progressIndicator) { currentUi.acquireLoginAndToken(server, executor, it) }
//      .completionOnEdt(progressIndicator.modalityState) { setBusy(false) }
//      .errorOnEdt(progressIndicator.modalityState) { setError(it) }
//  }
  suspend fun acquireLoginAndToken(): Pair<String, GECredentials> =
    withContext(Dispatchers.Main.immediate + ModalityState.stateForComponent(this).asContextElement()) {
      try {
        setBusy(true)
        tokenAcquisitionError = null
        currentUi.login(getServer())
      }
      catch (ce: CancellationException) {
        throw ce
      }
      catch (e: Exception) {
        setError(e)
        throw e
      }
      finally {
        setBusy(false)
      }
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
  }

  fun setLogin(login: String?, editable: Boolean) {
    passwordUi.setLogin(login.orEmpty(), editable)
    tokenUi.setFixedLogin(if (editable) null else login)
  }

  fun setCredentials(credentials: GECredentials?) {
    credentials ?.let {
      tokenUi.setFixedCredentials(credentials)
      refreshTokenUi.setFixedCredentials(credentials)
    }
  }

  fun setError(exception: Throwable?) {
    tokenAcquisitionError = exception?.let {
      currentUi.handleAcquireError(it)
    }
  }

  fun setOAuthUi() = applyUi(oauthUi)
  fun setPasswordUi() = applyUi(passwordUi)
  fun setTokenUi() = applyUi(tokenUi)
  fun setRefreshTokenUi() = applyUi(refreshTokenUi)

}