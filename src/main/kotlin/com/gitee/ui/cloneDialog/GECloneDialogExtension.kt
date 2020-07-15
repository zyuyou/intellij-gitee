// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.ui.cloneDialog

import com.gitee.api.GiteeApiRequestExecutorManager
import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.accounts.GiteeAccountInformationProvider
import com.gitee.authentication.accounts.GiteeAccountManager.Companion.ACCOUNT_REMOVED_TOPIC
import com.gitee.authentication.accounts.GiteeAccountManager.Companion.ACCOUNT_TOKEN_CHANGED_TOPIC
import com.gitee.authentication.accounts.isGiteeAccount
import com.gitee.i18n.GiteeBundle.message
import com.gitee.util.CachingGiteeUserAvatarLoader
import com.gitee.util.GiteeImageResizer
import com.gitee.util.GiteeUtil
import com.intellij.application.subscribe
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI.Borders.empty
import com.intellij.util.ui.JBUI.Panels.simplePanel
import com.intellij.util.ui.UIUtil.ComponentStyle
import com.intellij.util.ui.UIUtil.getRegularPanelInsets
import com.intellij.util.ui.cloneDialog.AccountMenuItem
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel

private fun getGEAccounts(): Collection<GiteeAccount> =
  GiteeAuthenticationManager.getInstance().getAccounts().filter { it.isGiteeAccount }

class GECloneDialogExtension : BaseCloneDialogExtension() {
  override fun getName() = GiteeUtil.SERVICE_DISPLAY_NAME

  override fun getAccounts(): Collection<GiteeAccount> = getGEAccounts()

  override fun createMainComponent(project: Project, modalityState: ModalityState): VcsCloneDialogExtensionComponent =
    object : BaseCloneDialogExtensionComponent(
      project,
      GiteeAuthenticationManager.getInstance(),
      GiteeApiRequestExecutorManager.getInstance(),
      GiteeAccountInformationProvider.getInstance(),
      CachingGiteeUserAvatarLoader.getInstance(),
      GiteeImageResizer.getInstance()
    ) {

      init {
        ACCOUNT_REMOVED_TOPIC.subscribe(this, this)
        ACCOUNT_TOKEN_CHANGED_TOPIC.subscribe(this, this)

        setup()
      }

      override fun getAccounts(): Collection<GiteeAccount> = getGEAccounts()

      override fun accountRemoved(removedAccount: GiteeAccount) {
        if (removedAccount.isGiteeAccount) super.accountRemoved(removedAccount)
      }

      override fun tokenChanged(account: GiteeAccount) {
        if (account.isGiteeAccount) super.tokenChanged(account)
      }

      override fun createLoginPanel(account: GiteeAccount?, cancelHandler: () -> Unit): JComponent =
        GHCloneDialogLoginPanel(account).apply {
          val chooseLoginUiHandler = { setChooseLoginUi() }
          loginPanel.setCancelHandler(if (getAccounts().isEmpty()) chooseLoginUiHandler else cancelHandler)
        }

      override fun createAccountMenuLoginActions(account: GiteeAccount?): Collection<AccountMenuItem.Action> =
        listOf(createLoginAction(account), createLoginWithTokenAction(account))

      private fun createLoginAction(account: GiteeAccount?): AccountMenuItem.Action {
        val isExistingAccount = account != null
        return AccountMenuItem.Action(
          if (isExistingAccount) message("login.action") else message("login.via.gitee.action"),
          {
            switchToLogin(account)
            getLoginPanel()?.setPasswordUi()
          },
          showSeparatorAbove = !isExistingAccount
        )
      }

      private fun createLoginWithTokenAction(account: GiteeAccount?): AccountMenuItem.Action {
        val isExistingAccount = account != null
        return AccountMenuItem.Action(
          if (isExistingAccount) message("login.with.token.action") else message("accounts.add.with.token"),
          {
            switchToLogin(account)
            getLoginPanel()?.setTokenUi()
          }
        )
      }

      private fun getLoginPanel(): GHCloneDialogLoginPanel? = content as? GHCloneDialogLoginPanel
    }
}

private class GHCloneDialogLoginPanel(account: GiteeAccount?) : JBPanel<GHCloneDialogLoginPanel>(VerticalLayout(0)) {
  private val titlePanel =
    simplePanel().apply {
      val title = JBLabel(message("login.to.gitee"), ComponentStyle.LARGE).apply { font = JBFont.label().biggerOn(5.0f) }
      addToLeft(title)
    }
  private val contentPanel = Wrapper()

  private val chooseLoginUiPanel: JPanel =
    JPanel(HorizontalLayout(0)).apply {
      border = JBEmptyBorder(getRegularPanelInsets())

      val loginViaGHButton = JButton(message("login.via.gitee.action")).apply { addActionListener { setPasswordUi() } }
      val useTokenLink = LinkLabel.create(message("link.label.use.token")) { setTokenUi() }

      add(loginViaGHButton)
      add(JBLabel(message("label.login.option.separator")).apply { border = empty(0, 6, 0, 4) })
      add(useTokenLink)
    }
  val loginPanel = CloneDialogLoginPanel(account).apply { setServer(GiteeServerPath.DEFAULT_HOST, false) }

  init {
    add(titlePanel.apply { border = JBEmptyBorder(getRegularPanelInsets().apply { bottom = 0 }) })
    add(contentPanel)

    setChooseLoginUi()
  }

  fun setChooseLoginUi() = setContent(chooseLoginUiPanel)

  fun setTokenUi() {
    setContent(loginPanel)
    loginPanel.setTokenUi() // after `loginPanel` is set as content to ensure correct focus behavior
  }

  fun setPasswordUi() {
    setContent(loginPanel)
    loginPanel.setPasswordUi() // after `loginPanel` is set as content to ensure correct focus behavior
  }

  private fun setContent(content: JComponent) {
    contentPanel.setContent(content)

    revalidate()
    repaint()
  }
}