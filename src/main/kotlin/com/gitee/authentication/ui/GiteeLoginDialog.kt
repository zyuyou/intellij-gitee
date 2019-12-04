// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.ui

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeServerPath
import com.gitee.util.GiteeAsyncUtil
import com.gitee.util.handleOnEdt
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Component
import javax.swing.JComponent
import javax.swing.JTextArea

class GiteeLoginDialog @JvmOverloads constructor(executorFactory: GiteeApiRequestExecutor.Factory,
                                                 project: Project?,
                                                 parent: Component? = null,
                                                 isAccountUnique: (name: String, server: GiteeServerPath) -> Boolean = { _, _ -> true },
                                                 title: String = "Log In to GitHub",
                                                 private val message: String? = null)
  : DialogWrapper(project, parent, false, IdeModalityType.PROJECT) {

  private var giteeLoginPanel = GiteeLoginPanel(executorFactory, isAccountUnique, project).apply {
    putClientProperty("isVisualPaddingCompensatedOnComponentLevel", false)
  }

  internal lateinit var login: String
  internal lateinit var accessToken: String
  internal lateinit var refreshToken: String

  init {
    this.title = title
    setOKButtonText("Log In")
    init()
  }

  @JvmOverloads
  fun withServer(path: String, editable: Boolean = true): GiteeLoginDialog {
    giteeLoginPanel.setServer(path, editable)
    return this
  }

  @JvmOverloads
  fun withCredentials(login: String? = null, password: String? = null, editableLogin: Boolean = true): GiteeLoginDialog {
    giteeLoginPanel.setCredentials(login, password, editableLogin)
    return this
  }

  @JvmOverloads
  fun withToken(token: String? = null): GiteeLoginDialog {
    giteeLoginPanel.setToken(token)
    return this
  }

  fun withError(exception: Throwable): GiteeLoginDialog {
    giteeLoginPanel.setError(exception)
    startTrackingValidation()
    return this
  }

  fun getServer(): GiteeServerPath = giteeLoginPanel.getServer()

  fun getLogin(): String = login

  fun getAccessToken(): String = accessToken
  fun getRefreshToken(): String = refreshToken

  override fun doOKAction() {
    val emptyProgressIndicator = EmptyProgressIndicator(ModalityState.stateForComponent(giteeLoginPanel))
    Disposer.register(disposable, Disposable { emptyProgressIndicator.cancel() })

    giteeLoginPanel.acquireLoginAndToken(emptyProgressIndicator).handleOnEdt { loginInfo, throwable ->
      if (throwable != null && !GiteeAsyncUtil.isCancellation(throwable)) startTrackingValidation()
      if (loginInfo != null) {
        login = loginInfo.first
        accessToken = loginInfo.second
        refreshToken = loginInfo.third
        close(OK_EXIT_CODE, true)
      }
    }
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

  override fun createSouthAdditionalPanel() = JBUI.Panels.simplePanel()
          .addToCenter(LinkLabel.create("Sign up for Gitee", Runnable { BrowserUtil.browse("https://gitee.com") }))
          .addToRight(JBLabel(AllIcons.Ide.External_link_arrow))

  override fun createCenterPanel(): Wrapper = giteeLoginPanel

  override fun getPreferredFocusedComponent(): JComponent = giteeLoginPanel.getPreferredFocus()

  override fun doValidateAll() = giteeLoginPanel.doValidateAll()
}
