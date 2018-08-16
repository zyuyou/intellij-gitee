// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.ui

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.authentication.accounts.GiteeAccountInformationProvider
import com.gitee.authentication.ui.GiteeAccountsPanel
import com.gitee.util.GiteeSettings
import com.intellij.openapi.Disposable
import com.intellij.openapi.options.ConfigurableUi
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UI.PanelFactory.grid
import com.intellij.util.ui.UI.PanelFactory.panel
import com.intellij.util.ui.UIUtil
import java.awt.Component.LEFT_ALIGNMENT
import java.awt.GridLayout
import java.text.NumberFormat
import javax.swing.*
import javax.swing.text.NumberFormatter

class GiteeSettingsPanel(project: Project,
                         executorFactory: GiteeApiRequestExecutor.Factory,
                         accountInformationProvider: GiteeAccountInformationProvider)
  : ConfigurableUi<GiteeSettingsConfigurable.GiteeSettingsHolder>, Disposable {

  private val accountsPanel = GiteeAccountsPanel(project, executorFactory, accountInformationProvider)

  private val timeoutField = JFormattedTextField(NumberFormatter(NumberFormat.getIntegerInstance()).apply {
    minimum = 0
    maximum = 60
  }).apply {
    columns = 4
    UIUtil.fixFormattedField(this)
  }

  private val cloneUsingSshCheckBox = JBCheckBox("Clone git repositories using ssh")

  override fun reset(settings: GiteeSettingsConfigurable.GiteeSettingsHolder) {
    val accountsMap = settings.applicationAccounts.accounts.map {
      it to settings.applicationAccounts.getTokensForAccount(it)
    }.toMap()

    accountsPanel.setAccounts(accountsMap, settings.projectAccount.account)
    accountsPanel.clearNewTokens()

    accountsPanel.loadExistingAccountsDetails()

    timeoutField.value = settings.application.getConnectionTimeoutSeconds()
    cloneUsingSshCheckBox.isSelected = settings.application.isCloneGitUsingSsh
  }

  override fun isModified(settings: GiteeSettingsConfigurable.GiteeSettingsHolder): Boolean {
    return timeoutField.value != settings.application.getConnectionTimeoutSeconds()
      || cloneUsingSshCheckBox.isSelected != settings.application.isCloneGitUsingSsh
      || accountsPanel.isModified(settings.applicationAccounts.accounts, settings.projectAccount.account)
  }

  override fun apply(settings: GiteeSettingsConfigurable.GiteeSettingsHolder) {
    val (accountsTokenMap, defaultAccount) = accountsPanel.getAccounts()

    settings.applicationAccounts.accounts = accountsTokenMap.keys
//    accountsTokenMap.filterValues { it != null }.forEach(settings.applicationAccounts::updateAccountToken)
    accountsTokenMap.filterValues { it != null }.mapValues { "${it.value?.first}&${it.value?.second}" }.forEach(settings.applicationAccounts::updateAccountToken)

    settings.projectAccount.account = defaultAccount

    accountsPanel.clearNewTokens()

    settings.application.setConnectionTimeoutSeconds(timeoutField.value as Int)
    settings.application.isCloneGitUsingSsh = cloneUsingSshCheckBox.isSelected
  }

  private fun GiteeSettings.getConnectionTimeoutSeconds(): Int {
    return connectionTimeout / 1000
  }

  private fun GiteeSettings.setConnectionTimeoutSeconds(timeout: Int) {
    connectionTimeout = timeout * 1000
  }

  override fun getComponent(): JComponent {
    val timeoutPanel = JPanel().apply {
      layout = BoxLayout(this, BoxLayout.X_AXIS)

      add(JLabel("Connection timeout:"))
      add(Box.createRigidArea(JBDimension(UIUtil.DEFAULT_HGAP, 0)))

      add(timeoutField)
      add(Box.createRigidArea(JBDimension(UIUtil.DEFAULT_HGAP, 0)))
      add(JLabel("seconds"))

      alignmentX = LEFT_ALIGNMENT
    }

    val settingsPanel = grid()
      .add(panel(cloneUsingSshCheckBox.apply { alignmentX = LEFT_ALIGNMENT }))
      .add(panel(timeoutPanel).resizeX(false))
      .createPanel()
      .apply {
        border = JBUI.Borders.empty(UIUtil.DEFAULT_VGAP, UIUtil.DEFAULT_HGAP)
      }

    return JPanel().apply {
      layout = GridLayout(2, 1)
      add(accountsPanel)
      add(settingsPanel)
    }
  }

  override fun dispose() {
    Disposer.dispose(accountsPanel)
  }
}
