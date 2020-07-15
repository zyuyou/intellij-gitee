// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.ui.cloneDialog

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.ui.GiteeLoginPanel
import com.gitee.authentication.ui.setPasswordUi
import com.gitee.authentication.ui.setTokenUi
import com.gitee.i18n.GiteeBundle.message
import com.gitee.util.completionOnEdt
import com.gitee.util.errorOnEdt
import com.gitee.util.successOnEdt
import com.intellij.ide.IdeBundle
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonShortcuts.ENTER
import com.intellij.openapi.actionSystem.PlatformDataKeys.CONTEXT_COMPONENT
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes.ERROR_ATTRIBUTES
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI.Borders.empty
import com.intellij.util.ui.JBUI.emptyInsets
import com.intellij.util.ui.UIUtil.getRegularPanelInsets
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

internal class CloneDialogLoginPanel(private val account: GiteeAccount?) :
  JBPanel<CloneDialogLoginPanel>(VerticalLayout(0)) {

  private val authenticationManager get() = GiteeAuthenticationManager.getInstance()

  private val errorPanel = JPanel(VerticalLayout(10))
  private val loginPanel = GiteeLoginPanel(GiteeApiRequestExecutor.Factory.getInstance()) { name, server ->
    if (account == null) authenticationManager.isAccountUnique(name, server) else true
  }
  private val loginButton = JButton(message("button.login.mnemonic"))
  private val backLink = LinkLabel<Any?>(IdeBundle.message("button.back"), null)

  var isCancelVisible: Boolean
    get() = backLink.isVisible
    set(value) {
      backLink.isVisible = value
    }

  init {
    buildLayout()

    if (account != null) {
      loginPanel.setCredentials(account.name, null, false)
      loginPanel.setServer(account.server.toUrl(), false)
    }

    loginButton.addActionListener { login() }
    LoginAction().registerCustomShortcutSet(ENTER, loginPanel)
  }

  fun setCancelHandler(listener: () -> Unit) = backLink.setListener({ _, _ -> listener() }, null)

  fun setTokenUi() = loginPanel.setTokenUi()
  fun setPasswordUi() = loginPanel.setPasswordUi()
  fun setServer(path: String, editable: Boolean) = loginPanel.setServer(path, editable)

  private fun buildLayout() {
    loginPanel.footer = { buttonPanel() } // footer is used to put buttons in 2-nd column - align under text boxes

    add(loginPanel)
    add(errorPanel.apply { border = JBEmptyBorder(getRegularPanelInsets().apply { top = 0 }) })
  }

  private fun LayoutBuilder.buttonPanel() =
    row("") {
      cell {
        loginButton()
        backLink().withLargeLeftGap()
      }
    }

  private fun login() {
    val modalityState = ModalityState.stateForComponent(this)

    loginPanel.acquireLoginAndToken(EmptyProgressIndicator(modalityState))
      .completionOnEdt(modalityState) { errorPanel.removeAll() }
      .errorOnEdt(modalityState) {
        loginPanel.doValidateAll().forEach { errorPanel.add(toErrorComponent(it)) }

        errorPanel.revalidate()
        errorPanel.repaint()
      }
      .successOnEdt(modalityState) { (login, token) ->
        if (account != null) {
          authenticationManager.updateAccountToken(account, token)
        }
        else {
          authenticationManager.registerAccount(login, loginPanel.getServer().host, token)
        }
      }
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