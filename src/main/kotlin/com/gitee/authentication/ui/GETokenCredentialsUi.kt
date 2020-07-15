package com.gitee.authentication.ui

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequests
import com.gitee.api.GiteeServerPath
import com.gitee.authentication.util.GiteeTokenCreator
import com.gitee.exceptions.GiteeAuthenticationException
import com.gitee.exceptions.GiteeParseException
import com.gitee.i18n.GiteeBundle.message
import com.gitee.ui.util.DialogValidationUtils.notBlank
import com.gitee.ui.util.Validator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.layout.ComponentPredicate
import com.intellij.ui.layout.LayoutBuilder
import java.net.UnknownHostException
import javax.swing.JComponent
import javax.swing.JTextField
import javax.swing.event.DocumentEvent

internal class GETokenCredentialsUi(
  private val serverTextField: ExtendableTextField,
  val factory: GiteeApiRequestExecutor.Factory,
  val isAccountUnique: UniqueLoginPredicate
) : GECredentialsUi() {

  private val accessTokenTextField = JBTextField()
  private val refreshTokenTextField = JBTextField()

  private var fixedLogin: String? = null

  fun setToken(token: String) {
    accessTokenTextField.text = token
  }

  override fun LayoutBuilder.centerPanel() {
    row(message("credentials.server.field")) { serverTextField(pushX, growX) }
    row(message("credentials.access.token.field")) {
      cell {
        accessTokenTextField(
          comment = message("login.insufficient.scopes", GiteeTokenCreator.MASTER_SCOPES),
          constraints = *arrayOf(pushX, growX)
        )
      }
    }
    row(message("credentials.refresh.token.field")) {
      cell {
        refreshTokenTextField(
//          comment = message("login.insufficient.scopes", GiteeTokenCreator.MASTER_SCOPES),
          constraints = *arrayOf(pushX, growX)
        )
      }
    }
  }

//  private fun browseNewTokenUrl() = browse(buildNewTokenUrl(serverTextField.tryParseServer()!!))

  override fun getPreferredFocusableComponent(): JComponent = accessTokenTextField

  override fun getValidator(): Validator = { notBlank(accessTokenTextField, message("login.token.cannot.be.empty")) }

  override fun createExecutor() = factory.create(accessTokenTextField.text)

  override fun acquireLoginAndToken(
    server: GiteeServerPath,
    executor: GiteeApiRequestExecutor,
    indicator: ProgressIndicator
  ): Triple<String, String, String> {
    val login = acquireLogin(server, executor, indicator, isAccountUnique, fixedLogin)

    return Triple(login, accessTokenTextField.text, refreshTokenTextField.text)
  }

  override fun handleAcquireError(error: Throwable): ValidationInfo =
    when (error) {
      is GiteeParseException -> ValidationInfo(error.message ?: message("credentials.invalid.server.path"), serverTextField)
      else -> handleError(error)
    }

  override fun setBusy(busy: Boolean) {
    accessTokenTextField.isEnabled = !busy
  }

  fun setFixedLogin(fixedLogin: String?) {
    this.fixedLogin = fixedLogin
  }

  companion object {
    fun acquireLogin(
      server: GiteeServerPath,
      executor: GiteeApiRequestExecutor,
      indicator: ProgressIndicator,
      isAccountUnique: UniqueLoginPredicate,
      fixedLogin: String?
    ): String {

      val login = executor.execute(indicator, GiteeApiRequests.CurrentUser.get(server)).login

      if (fixedLogin != null && fixedLogin != login) throw GiteeAuthenticationException("Token should match username \"$fixedLogin\"")
      if (!isAccountUnique(login, server)) throw LoginNotUniqueException(login)

      return login
    }

    fun handleError(error: Throwable): ValidationInfo =
      when (error) {
        is LoginNotUniqueException -> ValidationInfo(message("login.account.already.added", error.login)).withOKEnabled()
        is UnknownHostException -> ValidationInfo(message("server.unreachable")).withOKEnabled()
        is GiteeAuthenticationException -> ValidationInfo(message("credentials.incorrect", error.message.orEmpty())).withOKEnabled()
        else -> ValidationInfo(message("credentials.invalid.auth.data", error.message.orEmpty())).withOKEnabled()
      }
  }

}

private val JTextField.serverValid: ComponentPredicate
  get() = object : ComponentPredicate() {
    override fun invoke(): Boolean = tryParseServer() != null

    override fun addListener(listener: (Boolean) -> Unit) =
      document.addDocumentListener(object : DocumentAdapter() {
        override fun textChanged(e: DocumentEvent) = listener(tryParseServer() != null)
      })
  }

private fun JTextField.tryParseServer(): GiteeServerPath? =
  try {
    GiteeServerPath.from(text.trim())
  }
  catch (e: GiteeParseException) {
    null
  }