package com.gitee.authentication.ui

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GEAccountsUtil
import com.gitee.authentication.GECredentials
import com.gitee.authentication.util.GESecurityUtil
import com.gitee.exceptions.GiteeAuthenticationException
import com.gitee.exceptions.GiteeParseException
import com.gitee.i18n.GiteeBundle.message
import com.gitee.ui.util.DialogValidationUtils.notBlank
import com.gitee.ui.util.Validator
import com.intellij.openapi.progress.runUnderIndicator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Panel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.UnknownHostException
import javax.swing.JComponent

internal class GETokenCredentialsUi(
  private val serverTextField: ExtendableTextField,
  val factory: GiteeApiRequestExecutor.Factory,
  val isAccountUnique: UniqueLoginPredicate
) : GECredentialsUi() {

  private val accessTokenTextField = JBTextField()
  private val refreshTokenTextField = JBTextField()
  private val isPrivateTokenCheckBox = JBCheckBox(message("credentials.is.private.token.field"), false)

  private var fixedLogin: String? = null
  private var fixedCredentials: GECredentials? = null

  fun setAccessToken(token: String) {
    accessTokenTextField.text = token
  }

  fun setRefreshToken(token: String) {
    refreshTokenTextField.text = token
  }

  override fun Panel.centerPanel() {
    row(message("credentials.server.field")) { cell(serverTextField).align(AlignX.FILL) }
    row(message("credentials.access.token.field")) {
      cell(accessTokenTextField)
        .comment(message("login.insufficient.scopes", GEAccountsUtil.APP_CLIENT_SCOPE))
        .align(AlignX.FILL)
    }
    row(message("credentials.refresh.token.field")) {
      cell(refreshTokenTextField)
        .align(AlignX.FILL)
    }
    row("") {
      cell(isPrivateTokenCheckBox)
        .align(AlignX.FILL)
    }
  }

  override fun getPreferredFocusableComponent(): JComponent = accessTokenTextField

  override fun getValidator(): Validator = {
    notBlank(accessTokenTextField, message("login.token.cannot.be.empty"))
      ?: if(isPrivateTokenCheckBox.isSelected) null else notBlank(refreshTokenTextField, message("login.token.cannot.be.empty"))
  }

  override suspend fun login(server: GiteeServerPath): Pair<String, GECredentials> =
    withContext(Dispatchers.Main.immediate) {
      val token = accessTokenTextField.text
      val executor = factory.create(token)
      val login = acquireLogin(server, executor, isAccountUnique, fixedLogin)
      Pair(login, fixedCredentials ?: GECredentials.createCredentials(accessTokenTextField.text, refreshTokenTextField.text, isPrivateTokenCheckBox.isSelected))
    }

  override fun handleAcquireError(error: Throwable): ValidationInfo =
    when (error) {
      is GiteeParseException -> ValidationInfo(
        error.message ?: message("credentials.invalid.server.path"),
        serverTextField
      )

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

    credentials?.let {
      setAccessToken(it.accessToken)
      setRefreshToken(it.refreshToken)
    }
  }

  companion object {
    suspend fun acquireLogin(
      server: GiteeServerPath,
      executor: GiteeApiRequestExecutor,
      isAccountUnique: UniqueLoginPredicate,
      fixedLogin: String?
    ): String {

//      val login = executor.execute(indicator, GiteeApiRequests.CurrentUser.get(server)).login
      val (details, _) = withContext(Dispatchers.IO) {
        runUnderIndicator {
          GESecurityUtil.loadCurrentUserWithScopes(executor, server)
        }
      }
//      if (scopes == null || !GESecurityUtil.isEnoughScopes(scopes))
//        throw GiteeAuthenticationException("Insufficient scopes granted to token.")

      val login = details.login
      if (fixedLogin != null && fixedLogin != login) throw GiteeAuthenticationException("Token should match username \"$fixedLogin\"")
      if (!isAccountUnique(login, server)) throw LoginNotUniqueException(login)

      return login
    }

    fun handleError(error: Throwable): ValidationInfo =
      when (error) {
        is LoginNotUniqueException ->
          ValidationInfo(
            message("login.account.already.added", error.login)
          ).withOKEnabled()
        is UnknownHostException ->
          ValidationInfo(message("server.unreachable")).withOKEnabled()
        is GiteeAuthenticationException ->
          ValidationInfo(
            message("credentials.incorrect", error.message.orEmpty())
          ).withOKEnabled()
        else ->
          ValidationInfo(message("credentials.invalid.auth.data", error.message.orEmpty())).withOKEnabled()
      }
  }

}