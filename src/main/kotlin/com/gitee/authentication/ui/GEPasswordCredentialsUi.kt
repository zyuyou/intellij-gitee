package com.gitee.authentication.ui

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GECredentials
import com.gitee.authentication.util.GiteeCredentialsCreator
import com.gitee.exceptions.GiteeAuthenticationException
import com.gitee.exceptions.GiteeLoginException
import com.gitee.exceptions.GiteeParseException
import com.gitee.i18n.GiteeBundle.message
import com.gitee.ui.util.DialogValidationUtils
import com.gitee.ui.util.DialogValidationUtils.notBlank
import com.gitee.ui.util.Validator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.layout.LayoutBuilder
import java.net.UnknownHostException
import javax.swing.JComponent
import javax.swing.JPasswordField
import javax.swing.JTextField

internal class GEPasswordCredentialsUi(
  private val serverTextField: ExtendableTextField,
  private val executorFactory: GiteeApiRequestExecutor.Factory,
  private val isAccountUnique: UniqueLoginPredicate,
  private val clientIdTextField: JBTextField,
  private val clientSecretTextField: JPasswordField) : GECredentialsUi() {

  private val EMAIL_LOGIN_PATTERN = Regex("[\\w!#\$%&'*+/=?^_`{|}~-]+(?:\\.[\\w!#\$%&'*+/=?^_`{|}~-]+)*@(?:[\\w](?:[\\w-]*[\\w])?\\.)+[\\w](?:[\\w-]*[\\w])?")
  private val PHONE_NUMBER_LOGIN_PATTERN = Regex("^(?:\\+?86)?1(?:3\\d{3}|5[^4\\D]\\d{2}|8\\d{3}|7(?:[35678]\\d{2}|4(?:0\\d|1[0-2]|9\\d))|9[01356789]\\d{2}|66\\d{2})\\d{6}\$")

  private val loginTextField = JBTextField()
  private val passwordField = JPasswordField()

  fun setLogin(login: String, editable: Boolean = true) {
    loginTextField.text = login
    loginTextField.isEditable = editable
  }

  fun setPassword(password: String) {
    passwordField.text = password
  }

  override fun LayoutBuilder.centerPanel() {
    row(message("credentials.server.field")) { serverTextField(pushX, growX) }
    row(message("credentials.server.client.id.field")) { clientIdTextField(pushX, growX) }
    row(message("credentials.server.client.secret.field")) { clientSecretTextField(pushX, growX) }
    row(message("credentials.login.field")) { loginTextField(pushX, growX) }
    row(message("credentials.password.field")) {
      passwordField(
        comment = message("credentials.password.not.saved"),
        constraints = arrayOf(pushX, growX)
      )
    }
  }

  override fun getPreferredFocusableComponent(): JComponent =
    if (loginTextField.isEditable && loginTextField.text.isEmpty()) loginTextField else passwordField

  override fun getValidator() =
    DialogValidationUtils.chain(
      { notBlank(loginTextField, message("credentials.login.cannot.be.empty")) },
      loginNameValidator(loginTextField),
      { notBlank(passwordField, message("credentials.password.cannot.be.empty")) }
    )

  override fun createExecutor(): GiteeApiRequestExecutor.NoAuth = executorFactory.create()

  override fun acquireLoginAndToken(
    server: GiteeServerPath,
    executor: GiteeApiRequestExecutor,
    indicator: ProgressIndicator
  ): Pair<String, GECredentials> {
    val login = loginTextField.text.trim()
    if (!isAccountUnique(login, server)) throw LoginNotUniqueException(login)

    val credentials = GiteeCredentialsCreator(server, executor, indicator).create(loginTextField.text, passwordField.password)
    return Pair(loginTextField.text, credentials)
  }

  override fun handleAcquireError(error: Throwable): ValidationInfo =
    when (error) {
      is LoginNotUniqueException -> ValidationInfo(message("login.account.already.added", loginTextField.text), loginTextField).withOKEnabled()
      is UnknownHostException -> ValidationInfo(message("server.unreachable")).withOKEnabled()
      is GiteeAuthenticationException -> ValidationInfo(message("credentials.incorrect", error.message.orEmpty())).withOKEnabled()
      is GiteeParseException -> ValidationInfo(error.message ?: message("credentials.invalid.server.path"), serverTextField)
      else -> ValidationInfo(message("credentials.invalid.auth.data", error.message.orEmpty())).withOKEnabled()
    }

  override fun setBusy(busy: Boolean) {
    loginTextField.isEnabled = !busy
    passwordField.isEnabled = !busy
  }

  private fun loginNameValidator(textField: JTextField): Validator {
    return {
      val text = textField.text
      try {
        if (PHONE_NUMBER_LOGIN_PATTERN.containsMatchIn(text)) {
          throw GiteeLoginException("Phone number is not supported for oauth2.")
        }

        if (!EMAIL_LOGIN_PATTERN.containsMatchIn(text)) {
          throw GiteeLoginException("Email support only.")
        }

        null
      } catch (e: Exception) {
        ValidationInfo("$text is not a valid login name:\n${e.message}", textField)
      }
    }
  }
}