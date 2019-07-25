/*
 *  Copyright 2016-2019 码云 - Gitee
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.gitee.authentication.ui

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequests
import com.gitee.api.GiteeServerPath
import com.gitee.authentication.util.GiteeTokenCreator
import com.gitee.exceptions.GiteeAuthenticationException
import com.gitee.exceptions.GiteeParseException
import com.gitee.ui.util.DialogValidationUtils.chain
import com.gitee.ui.util.DialogValidationUtils.notBlank
import com.gitee.ui.util.Validator
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.Disposer
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.fields.ExtendableTextComponent
import com.intellij.ui.components.fields.ExtendableTextField
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UI.PanelFactory.grid
import com.intellij.util.ui.UI.PanelFactory.panel
import com.intellij.util.ui.UIUtil
import java.awt.Component
import java.awt.Cursor
import java.net.UnknownHostException
import javax.swing.*
import javax.swing.event.HyperlinkEvent
import javax.swing.text.html.HTMLEditorKit

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/authentication/ui/GithubLoginDialog.kt
 * @author JetBrains s.r.o.
 */
class GiteeLoginDialog @JvmOverloads constructor(private val executorFactory: GiteeApiRequestExecutor.Factory,
                                                 private val project: Project? = null,
                                                 parent: Component? = null,
                                                 private val isAccountUnique: (name: String, server: GiteeServerPath) -> Boolean = { _, _ -> true },
                                                 title: String = "Log In to Gitee",
                                                 private val message: String? = null)
  : DialogWrapper(project, parent, false, IdeModalityType.PROJECT) {

  private val serverTextField = ExtendableTextField(GiteeServerPath.DEFAULT_HOST)
  private var centerPanel = Wrapper()

  private var southAdditionalPanel = Wrapper().apply { JBUI.Borders.emptyRight(UIUtil.DEFAULT_HGAP) }

  private var passwordUi = LoginPasswordCredentialsUI()
  private var tokenUi = TokenCredentialsUI()

  private lateinit var currentUi: CredentialsUI

  private var progressIndicator: ProgressIndicator? = null
  private val progressIcon = AnimatedIcon.Default()
  private val progressExtension = ExtendableTextComponent.Extension { progressIcon }

  private lateinit var login: String
  private lateinit var accessToken: String
  private lateinit var refreshToken: String

//  var clientName: String = GiteeTokenCreator.DEFAULT_CLIENT_NAME

  private var tokenAcquisitionError: ValidationInfo? = null

  init {
    this.title = title
    setOKButtonText("Log In")

    applyUi(passwordUi)

    init()

    Disposer.register(disposable, Disposable { progressIndicator?.cancel() })
  }

  @JvmOverloads
  fun withServer(path: String, editable: Boolean = true): GiteeLoginDialog {
    serverTextField.apply {
      text = path
      isEditable = editable
    }
    return this
  }

  @JvmOverloads
  fun withCredentials(login: String? = null, password: String? = null): GiteeLoginDialog {
    if (login != null) passwordUi.setLogin(login)
    if (password != null) passwordUi.setPassword(password)
    applyUi(passwordUi)
    return this
  }

  @JvmOverloads
  fun withToken(token: String? = null): GiteeLoginDialog {
    if (token != null) tokenUi.setToken(token)
    applyUi(tokenUi)
    return this
  }

  fun getServer(): GiteeServerPath = GiteeServerPath.from(serverTextField.text)
  fun getLogin(): String = login.trim()
  fun getAccessToken(): String = accessToken.trim()
  fun getRefreshToken(): String = refreshToken.trim()

  override fun doOKAction() {
    setBusy(true)
    tokenAcquisitionError = null

    val server = getServer()
    val executor = currentUi.createExecutor()

    service<ProgressManager>().runProcessWithProgressAsynchronously(object : Task.Backgroundable(project, "Not Visible") {
      override fun run(indicator: ProgressIndicator) {
        val (newLogin, newAccessToken, newRefreshToken) = currentUi.acquireLoginAndToken(server, executor, indicator)
        login = newLogin
        accessToken = newAccessToken
        refreshToken = newRefreshToken
      }

      override fun onSuccess() {
        close(OK_EXIT_CODE, true)
        setBusy(false)
      }

      override fun onThrowable(error: Throwable) {
        startTrackingValidation()
        tokenAcquisitionError = currentUi.handleAcquireError(error)
        setBusy(false)
      }
    }, progressIndicator!!)
  }

  private fun setBusy(busy: Boolean) {
    if (busy) {
      if (!serverTextField.extensions.contains(progressExtension)) serverTextField.addExtension(progressExtension)
      progressIndicator?.cancel()
      progressIndicator = EmptyProgressIndicator(ModalityState.stateForComponent(window))
      Disposer.register(disposable, Disposable { progressIndicator?.cancel() })
    } else {
      serverTextField.removeExtension(progressExtension)
      progressIndicator = null
    }
    serverTextField.isEnabled = !busy
    currentUi.setBusy(busy)
  }

  override fun createNorthPanel(): JComponent? {
    return message?.let {
      JTextArea().apply {
        font = UIUtil.getLabelFont()
        text = it
        isEditable = false
        isFocusable = false
        isOpaque = false
        border = JBUI.Borders.emptyBottom(UIUtil.DEFAULT_VGAP * 2)
        margin = JBUI.emptyInsets()
      }
    }
  }

  override fun createSouthAdditionalPanel(): Wrapper = southAdditionalPanel
  override fun createCenterPanel(): Wrapper = centerPanel
  override fun getPreferredFocusedComponent(): JComponent = currentUi.getPreferredFocus()

  private fun applyUi(ui: CredentialsUI) {
    currentUi = ui

    centerPanel.setContent(currentUi.getPanel())
    southAdditionalPanel.setContent(currentUi.getSouthPanel())

    currentUi.getPreferredFocus().requestFocus()

    tokenAcquisitionError = null
  }

  override fun doValidateAll(): List<ValidationInfo> {
    return listOf(
      chain(
        chain({ notBlank(serverTextField, "Server cannot be empty") }, serverPathValidator(serverTextField)),
        currentUi.getValidator()
      ),
      { tokenAcquisitionError }
    ).mapNotNull { it() }
  }

  private fun serverPathValidator(textField: JTextField): Validator {
    return {
      val text = textField.text
      try {
        GiteeServerPath.from(text)
        null
      } catch (e: Exception) {
        ValidationInfo("$text is not a valid server path:\n${e.message}", textField)
      }
    }
  }

  private inner class LoginPasswordCredentialsUI : CredentialsUI() {
    private val loginTextField = JBTextField()

    private val passwordField = JPasswordField()
    private val contextHelp = JEditorPane()

    init {
      contextHelp.apply {
        editorKit = UIUtil.getHTMLEditorKit()
        val linkColor = JBUI.CurrentTheme.Link.linkColor()

        // language=CSS
        (editorKit as HTMLEditorKit).styleSheet.addRule("a {color: rgb(${linkColor.red}, ${linkColor.green}, ${linkColor.blue})}")

        // language=HTML
        text = "<html>Password is not saved and used only to <br>acquire Gitee accessToken. <a href=''>Enter accessToken</a></html>"

        addHyperlinkListener { e ->
          if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
            applyUi(tokenUi)
          } else {
            cursor = if (e.eventType == HyperlinkEvent.EventType.ENTERED) Cursor(Cursor.HAND_CURSOR) else Cursor(Cursor.DEFAULT_CURSOR)
          }
        }
        isEditable = false
        isFocusable = false
        isOpaque = false
        border = null
        margin = JBUI.emptyInsets()
        foreground = JBColor.GRAY
      }
    }

    fun setLogin(login: String) {
      loginTextField.text = login
    }

    fun setPassword(password: String) {
      passwordField.text = password
    }

    override fun getPanel() = grid()
      .add(panel(serverTextField).withLabel("Server:"))
      .add(panel(loginTextField).withLabel("Login:"))
      .add(panel(passwordField).withLabel("Password:"))
      .add(panel(contextHelp)).createPanel()

    override fun getPreferredFocus() = loginTextField

    override fun getValidator() = chain(
      { notBlank(loginTextField, "Login cannot be empty") },
      { notBlank(passwordField, "Password cannot be empty") }
    )

    override fun getSouthPanel() = JBUI.Panels.simplePanel()
      .addToCenter(LinkLabel.create("Sign up for Gitee") { BrowserUtil.browse("https://gitee.com") })
      .addToRight(JBLabel(AllIcons.Ide.External_link_arrow))

//    override fun createExecutor(): GiteeApiRequestExecutor.WithBasicAuth {
//      val modalityState = ModalityState.stateForComponent(passwordField)
//      return executorFactory.create(loginTextField.text, passwordField.password, Supplier {
//        invokeAndWaitIfNeed(modalityState) {
//          Messages.showInputDialog(passwordField,
//                                   "Authentication Code",
//                                   "Gitee Two-Factor Authentication",
//                                   null)
//        }
//      })
//    }

    override fun createExecutor(): GiteeApiRequestExecutor.WithPasswordOAuth2 {
      return executorFactory.create()
    }

    override fun acquireLoginAndToken(server: GiteeServerPath,
                                      executor: GiteeApiRequestExecutor,
                                      indicator: ProgressIndicator): Triple<String, String, String> {

      if (!isAccountUnique(loginTextField.text, server)) throw LoginNotUniqueException()

      val authorization = GiteeTokenCreator(server, executor, indicator).createMaster(loginTextField.text, passwordField.password)

      return Triple(loginTextField.text, authorization.accessToken, authorization.refreshToken)
    }

    override fun handleAcquireError(error: Throwable): ValidationInfo {
      return when (error) {
        is LoginNotUniqueException -> ValidationInfo("Account already added", loginTextField)
        is UnknownHostException -> ValidationInfo("Server is unreachable").withOKEnabled()
        is GiteeAuthenticationException -> ValidationInfo("Incorrect credentials.  ${error.message.orEmpty()}").withOKEnabled()
        is GiteeParseException -> ValidationInfo(error.message ?: "Invalid server path", serverTextField)
        else -> ValidationInfo("Invalid authentication data.\n ${error.message}").withOKEnabled()
      }
    }

    override fun setBusy(busy: Boolean) {
      loginTextField.isEnabled = !busy
      passwordField.isEnabled = !busy
      contextHelp.isEnabled = !busy
    }
  }

  private inner class TokenCredentialsUI : CredentialsUI() {
    private val GIST_SCOPE_PATTERN = Regex("(?:^|, )repo(?:,|$)")
    private val REPO_SCOPE_PATTERN = Regex("(?:^|, )gist(?:,|$)")

    private val accessTokenTextField = JBTextField()
    private val refreshTokenTextField = JBTextField()

    private val switchUiLink = LinkLabel.create("Log In with Username") { applyUi(passwordUi) }

    fun setToken(token: String) {
      accessTokenTextField.text = token
    }

    fun setTokens(tokens: Pair<String, String>) {
      accessTokenTextField.text = tokens.first
      refreshTokenTextField.text = tokens.second
    }

    override fun getPanel() = grid()
      .add(panel(serverTextField).withLabel("Server:"))
      .add(panel(accessTokenTextField).withLabel("Access Token:"))
      .add(panel(refreshTokenTextField).withLabel("Refresh Token:"))
      .createPanel()

    override fun getPreferredFocus() = accessTokenTextField

    override fun getValidator() = { notBlank(accessTokenTextField, "Token cannot be empty") }

    override fun getSouthPanel() = switchUiLink

    override fun createExecutor(): GiteeApiRequestExecutor {
      return executorFactory.create(accessTokenTextField.text)
    }

    override fun acquireLoginAndToken(server: GiteeServerPath,
                                      executor: GiteeApiRequestExecutor,
                                      indicator: ProgressIndicator): Triple<String, String, String> {

      val login = executor.execute(indicator, GiteeApiRequests.CurrentUser.get(server)).login

      // TODO checkout scopes
//      var scopes: String? = null
//      val login = executor.execute(indicator,
//        object : GiteeApiRequest.Get.Json<GiteeAuthenticatedUser>(
//          GiteeApiRequests.getUrl(server, GiteeApiRequests.CurrentUser.urlSuffix), GiteeAuthenticatedUser::class.java) {
//
//          override fun extractResult(response: GiteeApiResponse): GiteeAuthenticatedUser {
//            scopes = response.findHeader("X-OAuth-Scopes")
//            return super.extractResult(response)
//          }
//        }.withOperationName("get profile information")).login
//
//      if (scopes.isNullOrEmpty()
//        || !GIST_SCOPE_PATTERN.containsMatchIn(scopes!!)
//        || !REPO_SCOPE_PATTERN.containsMatchIn(scopes!!)) {
//        throw GiteeAuthenticationException("Access token should have `repo` and `gist` scopes.")
//      }

      if (!isAccountUnique(login, server)) throw LoginNotUniqueException(login)
      return Triple(login, accessTokenTextField.text, refreshTokenTextField.text)
    }

    override fun handleAcquireError(error: Throwable): ValidationInfo {
      return when (error) {
        is LoginNotUniqueException -> ValidationInfo("Account ${error.login} already added").withOKEnabled()
        is UnknownHostException -> ValidationInfo("Server is unreachable").withOKEnabled()
        is GiteeAuthenticationException -> ValidationInfo("Incorrect credentials.  ${error.message.orEmpty()}").withOKEnabled()
        is GiteeParseException -> ValidationInfo(error.message ?: "Invalid server path", serverTextField)
        else -> ValidationInfo("Invalid authentication data.\n ${error.message}").withOKEnabled()
      }
    }

    override fun setBusy(busy: Boolean) {
      accessTokenTextField.isEnabled = !busy
      refreshTokenTextField.isEnabled = !busy
      switchUiLink.isEnabled = !busy
    }
  }

  private abstract inner class CredentialsUI {
    abstract fun getPanel(): JPanel
    abstract fun getPreferredFocus(): JComponent
    abstract fun getSouthPanel(): JComponent
    abstract fun getValidator(): Validator
    abstract fun createExecutor(): GiteeApiRequestExecutor

    abstract fun acquireLoginAndToken(server: GiteeServerPath,
                                      executor: GiteeApiRequestExecutor,
                                      indicator: ProgressIndicator): Triple<String, String, String>

    abstract fun handleAcquireError(error: Throwable): ValidationInfo
    abstract fun setBusy(busy: Boolean)
  }

  private class LoginNotUniqueException(val login: String? = null) : RuntimeException()
}
