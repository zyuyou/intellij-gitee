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
package com.gitee.ui

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.authentication.ui.GiteeAccountsPanel
import com.gitee.util.CachingGiteeUserAvatarLoader
import com.gitee.util.GiteeImageResizer
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

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/ui/GithubSettingsPanel.kt
 * @author JetBrains s.r.o.
 */
class GiteeSettingsPanel(project: Project,
                         executorFactory: GiteeApiRequestExecutor.Factory,
                         avatarLoader: CachingGiteeUserAvatarLoader,
                         imageResizer: GiteeImageResizer)
  : ConfigurableUi<GiteeSettingsConfigurable.GiteeSettingsHolder>, Disposable {

  private val accountsPanel = GiteeAccountsPanel(project, executorFactory, avatarLoader, imageResizer)

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
    accountsTokenMap
      .filterValues { it != null }
      .mapValues { "${it.value?.first}&${it.value?.second}" }
      .forEach(settings.applicationAccounts::updateAccountToken)

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
