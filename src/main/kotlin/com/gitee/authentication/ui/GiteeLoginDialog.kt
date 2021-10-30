// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.ui

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.i18n.GiteeBundle
import com.intellij.openapi.project.Project
import com.intellij.ui.components.BrowserLink
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.Nls
import java.awt.Component
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextArea

internal class GiteeLoginDialog @JvmOverloads constructor(
  executorFactory: GiteeApiRequestExecutor.Factory,
  project: Project?,
  parent: Component? = null,
  isAccountUnique: UniqueLoginPredicate = { _, _ -> true },
  @Nls(capitalization = Nls.Capitalization.Title) title: String = GiteeBundle.message("login.to.gitee"),
  @Nls(capitalization = Nls.Capitalization.Sentence) private val message: String? = null
) : BaseLoginDialog(project, parent, executorFactory, isAccountUnique) {

  private val switchLoginUiLink = loginPanel.createSwitchUiLink()

  init {
    this.title = title
    setOKButtonText("Log In")
    init()
  }

  @JvmOverloads
  fun withServer(path: String, editable: Boolean = true): GiteeLoginDialog = apply { setServer(path, editable) }

  @JvmOverloads
  fun withToken(token: String? = null): GiteeLoginDialog = apply { loginPanel.setToken(token) }

  fun withError(exception: Throwable): GiteeLoginDialog =
    apply {
      loginPanel.setError(exception)
      startTrackingValidation()
    }

  override fun startGettingToken() {
    switchLoginUiLink.isEnabled = false
  }

  override fun finishGettingToken() {
    switchLoginUiLink.isEnabled = true
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

  override fun createSouthAdditionalPanel(): JPanel = createSignUpLink()

  override fun createCenterPanel(): JComponent =
    JBUI.Panels.simplePanel()
      .addToTop(
        JBUI.Panels.simplePanel().apply {
          border = JBEmptyBorder(UIUtil.getRegularPanelInsets().apply { bottom = 0 })

          addToRight(switchLoginUiLink)
        }
      )
      .addToCenter(loginPanel)
      .setPaddingCompensated()

  companion object {
    fun createSignUpLink(): JPanel = JBUI.Panels.simplePanel()
      .addToCenter(BrowserLink(GiteeBundle.message("login.sign.up"), "https://gitee.com"))
  }
}
