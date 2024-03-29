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

import com.gitee.authentication.GEAccountsUtil
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.i18n.GiteeBundle.message
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.NlsSafe
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.AlignY
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent
import javax.swing.JTextArea

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/ui/GithubCreateGistDialog.java
 * @author JetBrains s.r.o.
 */
class GiteeCreateGistDialog(private val project: Project,
                            @NlsSafe fileName: String?,
                            secret: Boolean,
                            openInBrowser: Boolean,
                            copyLink: Boolean) : DialogWrapper(project, true) {

  private val fileNameField: JBTextField? = fileName?.let { JBTextField(it) }
  private val descriptionField = JTextArea()
  private val secretCheckBox = JBCheckBox("Secret", secret)
  private val browserCheckBox = JBCheckBox("Open in browser", openInBrowser)
  private val copyLinkCheckBox = JBCheckBox("Copy URL", copyLink)

//  private val accountsModel = GEAccountsComboBoxModel(accounts, defaultAccount ?: accounts.firstOrNull())
  private val accounts = GEAccountsUtil.accounts

  private val accountsModel = CollectionComboBoxModel(
    accounts.toMutableList(),
    GEAccountsUtil.getDefaultAccount(project) ?: accounts.firstOrNull()
  )

  val fileName: String? get() = fileNameField?.text
  val description: String get() = descriptionField.text
  val isSecret: Boolean get() = secretCheckBox.isSelected
  val isOpenInBrowser: Boolean get() = browserCheckBox.isSelected
  val isCopyURL: Boolean get() = copyLinkCheckBox.isSelected
  val account: GiteeAccount? get() = accountsModel.selected

  init {
    title = message("create.gist.dialog.title")
    init()
  }

  override fun createCenterPanel() = panel {
    fileNameField?.let {
      row(message("create.gist.dialog.filename.field")) {
        cell(it).align(AlignX.FILL)
      }
    }
    row {
      label(message("create.gist.dialog.description.field"))
        .align(AlignY.TOP)
      scrollCell(descriptionField)
        .align(Align.FILL)
    }
    row("") {
      cell(secretCheckBox)
      cell(browserCheckBox)
      cell(copyLinkCheckBox)
    }

    if (accountsModel.size != 1) {
      row(message("create.gist.dialog.create.for.field")) {
        comboBox(accountsModel)
          .align(AlignX.FILL)
          .validationOnApply { if (accountsModel.selected == null) kotlin.error(message("dialog.message.account.cannot.be.empty")) else null }
          .resizableColumn()

        if (accountsModel.size == 0) {
          cell(GEAccountsUtil.createAddAccountLink(project, accountsModel))
        }
      }
    }
  }

  override fun getHelpId(): String = "gitee.create.gist.dialog"
  override fun getDimensionServiceKey(): String = "Gitee.CreateGistDialog"
  override fun getPreferredFocusedComponent(): JComponent = descriptionField
}
