// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.ui.cloneDialog

import com.gitee.api.GiteeApiRequestExecutorManager
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
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBFont
import com.intellij.util.ui.JBUI.Panels.simplePanel
import com.intellij.util.ui.UIUtil.ComponentStyle
import com.intellij.util.ui.UIUtil.getRegularPanelInsets
import com.intellij.util.ui.cloneDialog.AccountMenuItem
import com.intellij.util.ui.components.BorderLayoutPanel
import javax.swing.JComponent

private val GiteeAccount.isGEEAccount: Boolean get() = !isGiteeAccount

private fun getGEEAccounts(): Collection<GiteeAccount> =
  GiteeAuthenticationManager.getInstance().getAccounts().filter { it.isGEEAccount }

class GEECloneDialogExtension : BaseCloneDialogExtension() {
  override fun getName(): String = GiteeUtil.ENTERPRISE_SERVICE_DISPLAY_NAME

  override fun getAccounts(): Collection<GiteeAccount> = getGEEAccounts()

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

      override fun getAccounts(): Collection<GiteeAccount> = getGEEAccounts()

      override fun accountRemoved(removedAccount: GiteeAccount) {
        if (removedAccount.isGEEAccount) super.accountRemoved(removedAccount)
      }

      override fun tokenChanged(account: GiteeAccount) {
        if (account.isGEEAccount) super.tokenChanged(account)
      }

      override fun createLoginPanel(account: GiteeAccount?, cancelHandler: () -> Unit): JComponent =
        GEECloneDialogLoginPanel(account).apply {
          loginPanel.isCancelVisible = getAccounts().isNotEmpty()
          loginPanel.setCancelHandler(cancelHandler)
        }

      override fun createAccountMenuLoginActions(account: GiteeAccount?): Collection<AccountMenuItem.Action> =
        listOf(createLoginAction(account))

      private fun createLoginAction(account: GiteeAccount?): AccountMenuItem.Action {
        val isExistingAccount = account != null
        return AccountMenuItem.Action(
          if (isExistingAccount) message("login.action") else message("accounts.add"),
          { switchToLogin(account) },
          showSeparatorAbove = !isExistingAccount
        )
      }
    }
}

private class GEECloneDialogLoginPanel(account: GiteeAccount?) : BorderLayoutPanel() {
  private val titlePanel =
    simplePanel().apply {
      val title = JBLabel(message("login.to.gitee.enterprise"), ComponentStyle.LARGE).apply { font = JBFont.label().biggerOn(5.0f) }
      addToLeft(title)
    }
  val loginPanel = CloneDialogLoginPanel(account).apply {
    if (account == null) setServer("", true)
//    setTokenUi()
    setPasswordUi()
  }

  init {
    addToTop(titlePanel.apply { border = JBEmptyBorder(getRegularPanelInsets().apply { bottom = 0 }) })
    addToCenter(loginPanel)
  }
}