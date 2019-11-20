// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.ui

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequests
import com.gitee.api.GiteeServerPath
import com.gitee.authentication.util.GiteeTokenCreator
import com.gitee.exceptions.GiteeAuthenticationException
import com.gitee.exceptions.GiteeParseException
import com.gitee.ui.util.DialogValidationUtils
import com.gitee.ui.util.Validator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.panel
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import java.net.UnknownHostException
import javax.swing.*

sealed class GiteeCredentialsUI {
  abstract fun getPanel(): JPanel
  abstract fun getPreferredFocus(): JComponent
  abstract fun getValidator(): Validator
  abstract fun createExecutor(): GiteeApiRequestExecutor
  abstract fun acquireLoginAndToken(server: GiteeServerPath,
                                    executor: GiteeApiRequestExecutor,
                                    indicator: ProgressIndicator): Triple<String, String, String>

  abstract fun handleAcquireError(error: Throwable): ValidationInfo
  abstract fun setBusy(busy: Boolean)

  protected val loginButton = JButton("Log In").apply { isVisible = false }
  protected val cancelButton = JButton("Cancel").apply { isVisible = false }

  open fun setLoginAction(actionListener: ActionListener) {
    loginButton.addActionListener(actionListener)
    loginButton.setMnemonic('l')
  }

  fun setCancelAction(actionListener: ActionListener) {
    cancelButton.addActionListener(actionListener)
    cancelButton.setMnemonic('c')
  }

  fun setLoginButtonVisible(visible: Boolean) {
    loginButton.isVisible = visible
  }

  fun setCancelButtonVisible(visible: Boolean) {
    cancelButton.isVisible = visible
  }

  internal class PasswordUI(private val serverTextField: ExtendableTextField,
                            private val clientName: String,
                            switchUi: () -> Unit,
                            private val executorFactory: GiteeApiRequestExecutor.Factory,
                            private val isAccountUnique: (login: String, server: GiteeServerPath) -> Boolean,
                            private val dialogMode: Boolean) : GiteeCredentialsUI() {
    private val loginTextField = JBTextField()
    private val passwordField = JPasswordField()
    private val switchUiLink = LinkLabel.create("Use Token", switchUi)

    fun setLogin(login: String, editable: Boolean = true) {
      loginTextField.text = login
      loginTextField.isEditable = editable
    }

    fun setPassword(password: String) {
      passwordField.text = password
    }

    override fun setLoginAction(actionListener: ActionListener) {
      super.setLoginAction(actionListener)
      passwordField.setEnterPressedAction(actionListener)
      loginTextField.setEnterPressedAction(actionListener)
      serverTextField.setEnterPressedAction(actionListener)
    }

    override fun getPanel(): JPanel = panel {
      buildTitleAndLinkRow(this, dialogMode, switchUiLink)
      row("Server:") { serverTextField(pushX, growX) }
      row("Login:") { loginTextField(pushX, growX) }
      row("Password:") {
        passwordField(comment = "Password is not saved and used only to acquire Gitee token",
                      constraints = *arrayOf(pushX, growX))
      }
      row("") {
        cell {
          loginButton()
          cancelButton()
        }
      }
    }.apply {
      border = JBUI.Borders.empty(UIUtil.REGULAR_PANEL_TOP_BOTTOM_INSET, UIUtil.REGULAR_PANEL_LEFT_RIGHT_INSET)
    }

    override fun getPreferredFocus() = if (loginTextField.isEditable && loginTextField.text.isEmpty()) loginTextField else passwordField

    override fun getValidator() = DialogValidationUtils.chain(
      { DialogValidationUtils.notBlank(loginTextField, "Login cannot be empty") },
      { DialogValidationUtils.notBlank(passwordField, "Password cannot be empty") })


//    override fun createExecutor(): GiteeApiRequestExecutor.WithBasicAuth {
//      val modalityState = ModalityState.stateForComponent(passwordField)
//
//
//      return executorFactory.create(loginTextField.text, passwordField.password, Supplier {
//        invokeAndWaitIfNeeded(modalityState) {
//          Messages.showInputDialog(passwordField,
//                                   "Authentication code:",
//                                   "Gitee Two-Factor Authentication",
//                                   null)
//        }
//      })
//    }

    override fun createExecutor(): GiteeApiRequestExecutor.WithPasswordOAuth2 {
      return executorFactory.create()
    }

//    override fun acquireLoginAndToken(server: GiteeServerPath,
//                                      executor: GiteeApiRequestExecutor,
//                                      indicator: ProgressIndicator): Pair<String, String> {
//
//      val login = loginTextField.text.trim()
//      if (!isAccountUnique(login, server)) throw LoginNotUniqueException(login)
//      val token = GiteeTokenCreator(server, executor, indicator).createMaster(clientName).token
//      return login to token
//    }

    override fun acquireLoginAndToken(server: GiteeServerPath,
                                      executor: GiteeApiRequestExecutor,
                                      indicator: ProgressIndicator): Triple<String, String, String> {

      val login = loginTextField.text.trim()

      if (!isAccountUnique(login, server)) throw LoginNotUniqueException(login)

      val authorization = GiteeTokenCreator(server, executor, indicator).createMaster(loginTextField.text, passwordField.password)

      return Triple(loginTextField.text, authorization.accessToken, authorization.refreshToken)
    }

    override fun handleAcquireError(error: Throwable): ValidationInfo {
      return when (error) {
        is LoginNotUniqueException -> ValidationInfo("Account already added", loginTextField).withOKEnabled()
        is UnknownHostException -> ValidationInfo("Server is unreachable").withOKEnabled()
        is GiteeAuthenticationException -> ValidationInfo("Incorrect credentials. ${error.message.orEmpty()}").withOKEnabled()
        is GiteeParseException -> ValidationInfo(error.message ?: "Invalid server path", serverTextField)
        else -> ValidationInfo("Invalid authentication data.\n ${error.message}").withOKEnabled()
      }
    }

    override fun setBusy(busy: Boolean) {
      loginTextField.isEnabled = !busy
      passwordField.isEnabled = !busy
      switchUiLink.isEnabled = !busy
    }
  }

  internal class TokenUI(val factory: GiteeApiRequestExecutor.Factory,
                         val isAccountUnique: (name: String, server: GiteeServerPath) -> Boolean,
                         private val serverTextField: ExtendableTextField,
                         switchUi: () -> Unit,
                         private val dialogMode: Boolean) : GiteeCredentialsUI() {
    private val GIST_SCOPE_PATTERN = Regex("(?:^|, )repo(?:,|$)")
    private val REPO_SCOPE_PATTERN = Regex("(?:^|, )gist(?:,|$)")

    private val accessTokenTextField = JBTextField()
    private val refreshTokenTextField = JBTextField()

    private val switchUiLink = LinkLabel.create("Use Credentials", switchUi)
    private var fixedLogin: String? = null

    fun setToken(token: String) {
      accessTokenTextField.text = token
    }

    fun setTokens(tokens: Pair<String, String>) {
      accessTokenTextField.text = tokens.first
      refreshTokenTextField.text = tokens.second
    }

    override fun getPanel() = panel {
      buildTitleAndLinkRow(this, dialogMode, switchUiLink)
      row("Server:") { serverTextField(pushX, growX) }
      row("Access Token:") { accessTokenTextField(pushX, growX) }
      row("Refresh Token:") { refreshTokenTextField(pushX, growX) }
      row("") {
        cell {
          loginButton()
          cancelButton()
        }
      }
    }.apply {
      border = JBUI.Borders.empty(UIUtil.REGULAR_PANEL_TOP_BOTTOM_INSET, UIUtil.REGULAR_PANEL_LEFT_RIGHT_INSET)
    }

    override fun getPreferredFocus() = accessTokenTextField

    override fun getValidator(): () -> ValidationInfo? = {
      DialogValidationUtils.notBlank(accessTokenTextField, "Token cannot be empty")
    }

    override fun createExecutor() = factory.create(accessTokenTextField.text)

//    override fun acquireLoginAndToken(server: GiteeServerPath,
//                                      executor: GiteeApiRequestExecutor,
//                                      indicator: ProgressIndicator): Pair<String, String> {
//      var scopes: String? = null
//      val login = executor.execute(indicator,
//                                   object : GiteeApiRequest.Get.Json<GiteeAuthenticatedUser>(
//                                     GiteeApiRequests.getUrl(server,
//                                                              GiteeApiRequests.CurrentUser.urlSuffix),
//                                     GiteeAuthenticatedUser::class.java) {
//                                     override fun extractResult(response: GiteeApiResponse): GiteeAuthenticatedUser {
//                                       scopes = response.findHeader("X-OAuth-Scopes")
//                                       return super.extractResult(response)
//                                     }
//                                   }.withOperationName("get profile information")).login
//      if (scopes.isNullOrEmpty()
//          || !GIST_SCOPE_PATTERN.containsMatchIn(scopes!!)
//          || !REPO_SCOPE_PATTERN.containsMatchIn(scopes!!)) {
//        throw GiteeAuthenticationException("Access token should have `repo` and `gist` scopes.")
//      }
//
//      fixedLogin?.let {
//        if (it != login) throw GiteeAuthenticationException("Token should match username \"$it\"")
//      }
//
//      if (!isAccountUnique(login, server)) throw LoginNotUniqueException(login)
//      return login to tokenTextField.text
//    }

    override fun acquireLoginAndToken(server: GiteeServerPath,
                                      executor: GiteeApiRequestExecutor,
                                      indicator: ProgressIndicator): Triple<String, String, String> {

      val login = executor.execute(indicator, GiteeApiRequests.CurrentUser.get(server)).login

      // TODO checkout scopes
//      var scopes: String? = null
//      val login = executor.execute(indicator,
//                                   object : GiteeApiRequest.Get.Json<GiteeAuthenticatedUser>(
//                                     GiteeApiRequests.getUrl(server,
//                                                              GiteeApiRequests.CurrentUser.urlSuffix),
//                                     GiteeAuthenticatedUser::class.java) {
//                                     override fun extractResult(response: GiteeApiResponse): GiteeAuthenticatedUser {
//                                       scopes = response.findHeader("X-OAuth-Scopes")
//                                       return super.extractResult(response)
//                                     }
//                                   }.withOperationName("get profile information")).login
//      if (scopes.isNullOrEmpty()
//          || !GIST_SCOPE_PATTERN.containsMatchIn(scopes!!)
//          || !REPO_SCOPE_PATTERN.containsMatchIn(scopes!!)) {
//        throw GiteeAuthenticationException("Access token should have `repo` and `gist` scopes.")
//      }
//
//      fixedLogin?.let {
//        if (it != login) throw GiteeAuthenticationException("Token should match username \"$it\"")
//      }

      if (!isAccountUnique(login, server)) throw LoginNotUniqueException(login)
      return Triple(login, accessTokenTextField.text, refreshTokenTextField.text)
    }

    override fun handleAcquireError(error: Throwable): ValidationInfo {
      return when (error) {
        is LoginNotUniqueException -> ValidationInfo("Account ${error.login} already added").withOKEnabled()
        is UnknownHostException -> ValidationInfo("Server is unreachable").withOKEnabled()
        is GiteeAuthenticationException -> ValidationInfo("Incorrect credentials. ${error.message.orEmpty()}").withOKEnabled()
        is GiteeParseException -> ValidationInfo(error.message ?: "Invalid server path", serverTextField)
        else -> ValidationInfo("Invalid authentication data.\n ${error.message}").withOKEnabled()
      }
    }

    override fun setBusy(busy: Boolean) {
      accessTokenTextField.isEnabled = !busy
      switchUiLink.isEnabled = !busy
    }

    fun setFixedLogin(fixedLogin: String?) {
      this.fixedLogin = fixedLogin
    }

    override fun setLoginAction(actionListener: ActionListener) {
      super.setLoginAction(actionListener)
      accessTokenTextField.setEnterPressedAction(actionListener)
      serverTextField.setEnterPressedAction(actionListener)
    }
  }
}

private fun buildTitleAndLinkRow(layoutBuilder: LayoutBuilder,
                                 dialogMode: Boolean,
                                 linkLabel: LinkLabel<*>) {
  layoutBuilder.row {
    cell(isFullWidth = true) {
      if (!dialogMode) {
        val jbLabel = JBLabel("Log In to Gitee", UIUtil.ComponentStyle.LARGE).apply {
          font = JBFont.label().biggerOn(5.0f)
        }
        jbLabel()
      }
      JLabel(" ")(pushX, growX) // just to be able to align link to the right
      linkLabel()
    }
  }
}

private fun JComponent.setEnterPressedAction(actionListener: ActionListener) {
  registerKeyboardAction(actionListener, KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), JComponent.WHEN_FOCUSED)
}
