// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.ui.cloneDialog

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.ui.GiteeLoginPanel
import com.gitee.i18n.GiteeBundle.message
import com.gitee.util.completionOnEdt
import com.gitee.util.errorOnEdt
import com.gitee.util.successOnEdt
import com.intellij.ide.IdeBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonShortcuts.ENTER
import com.intellij.openapi.actionSystem.PlatformDataKeys.CONTEXT_COMPONENT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.ComponentValidator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes.ERROR_ATTRIBUTES
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI.Borders.empty
import com.intellij.util.ui.JBUI.Panels.simplePanel
import com.intellij.util.ui.JBUI.emptyInsets
import com.intellij.util.ui.UIUtil.getRegularPanelInsets
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

internal class CloneDialogLoginPanel(private val account: GiteeAccount?) :
  JBPanel<CloneDialogLoginPanel>(VerticalLayout(0)), Disposable {

  private val authenticationManager get() = GiteeAuthenticationManager.getInstance()

  private val errorPanel = JPanel(VerticalLayout(10))
  private val loginPanel = GiteeLoginPanel(GiteeApiRequestExecutor.Factory.getInstance()) { name, server ->
    if (account == null) authenticationManager.isAccountUnique(name, server) else true
  }
  private val inlineCancelPanel = simplePanel()
  private val loginButton = JButton(message("button.login.mnemonic"))
  private val backLink = LinkLabel<Any?>(IdeBundle.message("button.back"), null).apply { verticalAlignment = SwingConstants.TOP }

  private var errors = emptyList<ValidationInfo>()
  private var loginIndicator: ProgressIndicator? = null

  var isCancelVisible: Boolean
    get() = backLink.isVisible
    set(value) {
      backLink.isVisible = value
    }

  init {
    buildLayout()

    if (account != null) {
      loginPanel.setServer(account.server.toUrl(), false)
      loginPanel.setLogin(account.name, false)
//      loginPanel.setCredentials(account.name, null, false)
    }

    loginButton.addActionListener { login() }
    LoginAction().registerCustomShortcutSet(ENTER, loginPanel)
  }

  fun setCancelHandler(listener: () -> Unit) =
    backLink.setListener({ _, _ -> listener() }, null)

  fun setTokenUi() {
    setupNewUi(false)
    loginPanel.setTokenUi()
  }

  fun setPasswordUi() {
    setupNewUi(false)
    loginPanel.setPasswordUi()
  }

  fun setServer(path: String, editable: Boolean) = loginPanel.setServer(path, editable)

  override fun dispose() = Unit

  private fun buildLayout() {
    add(JPanel(HorizontalLayout(0)).apply {
      add(loginPanel)
      add(inlineCancelPanel.apply { border = JBEmptyBorder(getRegularPanelInsets().apply { left = JBUIScale.scale(6) }) })
    })
    add(errorPanel.apply { border = JBEmptyBorder(getRegularPanelInsets().apply { top = 0 }) })

//    loginPanel.footer = { buttonPanel() } // footer is used to put buttons in 2-nd column - align under text boxes
//    add(loginPanel)
//    add(errorPanel.apply { border = JBEmptyBorder(getRegularPanelInsets().apply { top = 0 }) })
  }

  private fun setupNewUi(isOAuth: Boolean) {
    loginButton.isVisible = !isOAuth
    backLink.text = if (isOAuth) IdeBundle.message("link.cancel") else IdeBundle.message("button.back")

    loginPanel.footer = { if (!isOAuth) buttonPanel() } // footer is used to put buttons in 2-nd column - align under text boxes
    if (isOAuth) inlineCancelPanel.addToCenter(backLink)
    inlineCancelPanel.isVisible = isOAuth

    clearErrors()
  }

  private fun LayoutBuilder.buttonPanel() =
    row("") {
      cell {
        loginButton()
        backLink().withLargeLeftGap()
      }
    }

  fun cancelLogin() {
    loginIndicator?.cancel()
    loginIndicator = null
  }

  private fun login() {
    cancelLogin()

    loginPanel.setError(null)
    clearErrors()
    if (!doValidate()) return

    val modalityState = ModalityState.stateForComponent(this)
    val indicator = EmptyProgressIndicator(modalityState)

    loginIndicator = indicator

    loginPanel.acquireLoginAndToken(indicator)
      .completionOnEdt(modalityState) {
        loginIndicator = null
        clearErrors()
      }
      .errorOnEdt(modalityState) {
        doValidate()
      }
      .successOnEdt(modalityState) { (login, accessToken, refreshToken) ->
        val token = "${accessToken}&${refreshToken}"

        if (account != null) {
          authenticationManager.updateAccountToken(account, token)
        }
        else {
          authenticationManager.registerAccount(login, loginPanel.getServer(), token)
        }
      }
  }

  private fun doValidate(): Boolean {
    errors = loginPanel.doValidateAll()
    setErrors(errors)

    val toFocus = errors.firstOrNull()?.component
    if (toFocus?.isVisible == true) IdeFocusManager.getGlobalInstance().requestFocus(toFocus, true)

    return errors.isEmpty()
  }

  private fun clearErrors() {
    for (component in errors.mapNotNull { it.component }) {
      ComponentValidator.getInstance(component).ifPresent { it.updateInfo(null) }
    }
    errorPanel.removeAll()
    errors = emptyList()
  }

  private fun setErrors(errors: Collection<ValidationInfo>) {
    for (error in errors) {
      val component = error.component

      if (component != null) {
        ComponentValidator.getInstance(component)
          .orElseGet { ComponentValidator(this).installOn(component) }
          .updateInfo(error)
      }
      else {
        errorPanel.add(toErrorComponent(error))
      }
    }

    errorPanel.revalidate()
    errorPanel.repaint()
  }

  private fun toErrorComponent(info: ValidationInfo): JComponent =
    SimpleColoredComponent().apply {
      myBorder = empty()
      ipad = emptyInsets()

      append(info.message, ERROR_ATTRIBUTES)
    }

  private inner class LoginAction : DumbAwareAction() {
    override fun update(e: AnActionEvent) {
      e.presentation.isEnabledAndVisible = e.getData(CONTEXT_COMPONENT) != backLink
    }

    override fun actionPerformed(e: AnActionEvent) = login()
  }
}