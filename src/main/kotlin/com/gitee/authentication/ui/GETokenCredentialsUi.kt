package com.gitee.authentication.ui

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequests
import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GECredentials
import com.gitee.authentication.accounts.GEAccountsUtils
import com.gitee.exceptions.GiteeAuthenticationException
import com.gitee.exceptions.GiteeParseException
import com.gitee.i18n.GiteeBundle.message
import com.gitee.ui.util.DialogValidationUtils.notBlank
import com.gitee.ui.util.Validator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.layout.LayoutBuilder
import java.net.UnknownHostException
import javax.swing.JComponent

internal class GETokenCredentialsUi(
  private val serverTextField: ExtendableTextField,
  val factory: GiteeApiRequestExecutor.Factory,
  val isAccountUnique: UniqueLoginPredicate
) : GECredentialsUi() {

  private val accessTokenTextField = JBTextField()
  private val refreshTokenTextField = JBTextField()

  private var fixedLogin: String? = null
  private var fixedCredentials: GECredentials? = null

  fun setAccessToken(token: String) {
    accessTokenTextField.text = token
  }

  fun setRefreshToken(token: String) {
    refreshTokenTextField.text = token
  }

  override fun LayoutBuilder.centerPanel() {
    row(message("credentials.server.field")) {
      serverTextField(pushX, growX)
    }
    row(message("credentials.access.token.field")) {
      cell {
        accessTokenTextField(
          comment = message("login.insufficient.scopes", GEAccountsUtils.APP_CLIENT_SCOPE),
          constraints = arrayOf(pushX, growX)
        )
      }
    }
    row(message("credentials.refresh.token.field")) {
      cell {
        refreshTokenTextField(
          comment = message("login.insufficient.scopes", GEAccountsUtils.APP_CLIENT_SCOPE),
          constraints = arrayOf(pushX, growX)
        )
      }
    }
  }

  override fun getPreferredFocusableComponent(): JComponent = accessTokenTextField

  override fun getValidator(): Validator = {
    notBlank(accessTokenTextField, message("login.token.cannot.be.empty")) ?: notBlank(refreshTokenTextField, message("login.token.cannot.be.empty"))
  }

  override fun createExecutor() = factory.create(accessTokenTextField.text)

  override fun acquireLoginAndToken(
    server: GiteeServerPath,
    executor: GiteeApiRequestExecutor,
    indicator: ProgressIndicator
  ): Pair<String, GECredentials> {

    val login = acquireLogin(server, executor, indicator, isAccountUnique, fixedLogin)

    return Pair(login, fixedCredentials ?: GECredentials.createCredentials(accessTokenTextField.text, refreshTokenTextField.text))
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

  fun setFixedCredentials(credentials: GECredentials?) {
    fixedCredentials = credentials

    credentials ?.let {
      setAccessToken(it.accessToken)
      setRefreshToken(it.refreshToken)
    }
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