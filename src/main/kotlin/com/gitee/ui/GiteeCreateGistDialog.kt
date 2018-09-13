/*
 * Copyright 2016-2018 码云 - Gitee
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gitee.ui

import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.ui.GiteeAccountCombobox
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.panel.PanelGridBuilder
import com.intellij.ui.components.JBBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UI.PanelFactory.grid
import com.intellij.util.ui.UI.PanelFactory.panel
import com.intellij.util.ui.UIUtil
import javax.swing.Box
import javax.swing.JComponent
import javax.swing.JTextArea

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/ui/GithubCreateGistDialog.java
 * @author JetBrains s.r.o.
 */
class GiteeCreateGistDialog(val project: Project,
                            _accounts: Set<GiteeAccount>,
                            _defaultAccount: GiteeAccount?,
                            _fileName: String?,
                            _secret: Boolean,
                            _openInBrowser: Boolean,
                            _copyLink: Boolean) : DialogWrapper(project) {

  private val fileNameField: JBTextField? = _fileName?.let { JBTextField(it) }
  private val descriptionTextArea = JTextArea()
  private val secretCheckBox = JBCheckBox("Secret", _secret)
  private val openInBrowserCheckBox = JBCheckBox("Open in browser", _openInBrowser)
  private val copyLinkCheckBox = JBCheckBox("Copy URL", _copyLink)
  private val accountSelector = GiteeAccountCombobox(_accounts, _defaultAccount, null)

  init {
    title = "Create Gist"
    init()
  }

  override fun getHelpId(): String? {
    return "gitee.create.gist.dialog"
  }

  override fun getDimensionServiceKey(): String? {
    return "Gitee.CreateGistDialog"
  }

  override fun getPreferredFocusedComponent(): JComponent? {
    return descriptionTextArea
  }

  override fun createCenterPanel(): JComponent? {
    val checkBoxes = JBBox.createHorizontalBox()
    checkBoxes.add(secretCheckBox)
    checkBoxes.add(Box.createRigidArea(JBUI.size(UIUtil.DEFAULT_HGAP, 0)))
    checkBoxes.add(openInBrowserCheckBox)
    checkBoxes.add(Box.createRigidArea(JBUI.size(UIUtil.DEFAULT_HGAP, 0)))
    checkBoxes.add(copyLinkCheckBox)

    val descriptionPane = JBScrollPane(descriptionTextArea).apply {
      preferredSize = JBDimension(270, 60)
      minimumSize = JBDimension(270, 60)
    }

    return com.intellij.ui.layout.panel {
      if (fileNameField != null) {
        row("Filename:") {
          fileNameField.invoke()
        }
      }

      row("Description:") {
        descriptionPane()
      }

      row("") {
        checkBoxes()
      }

      if (accountSelector.isEnabled) {
        row("Create for:") {
          accountSelector()
        }
      }
    }
  }

  fun getDescription(): String {
    return descriptionTextArea.text
  }

  fun isSecret(): Boolean {
    return secretCheckBox.isSelected
  }

  fun isOpenInBrowser(): Boolean {
    return openInBrowserCheckBox.isSelected
  }

  fun isCopyURL(): Boolean {
    return copyLinkCheckBox.isSelected
  }

  fun getAccount(): GiteeAccount? {
    return accountSelector.selectedItem as? GiteeAccount
  }

  fun getFileName(): String? {
    return fileNameField?.text
  }

}
