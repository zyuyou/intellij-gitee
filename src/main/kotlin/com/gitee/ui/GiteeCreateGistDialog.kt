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

import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.ui.GEAccountsComboBoxModel
import com.gitee.authentication.ui.GEAccountsComboBoxModel.Companion.accountSelector
import com.gitee.authentication.ui.GEAccountsHost
import com.gitee.i18n.GiteeBundle.message
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.layout.panel
import javax.swing.JComponent
import javax.swing.JTextArea

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/ui/GithubCreateGistDialog.java
 * @author JetBrains s.r.o.
 */
class GiteeCreateGistDialog(project: Project,
                            accounts: Set<GiteeAccount>,
                            defaultAccount: GiteeAccount?,
                            fileName: String?,
                            secret: Boolean,
                            openInBrowser: Boolean,
                            copyLink: Boolean) : DialogWrapper(project, true), DataProvider {

  private val fileNameField: JBTextField? = fileName?.let { JBTextField(it) }
  private val descriptionField = JTextArea()
  private val secretCheckBox = JBCheckBox("Secret", secret)
  private val browserCheckBox = JBCheckBox("Open in browser", openInBrowser)
  private val copyLinkCheckBox = JBCheckBox("Copy URL", copyLink)

//  private val accountSelector = GiteeAccountCombobox(accounts, defaultAccount, null)
  private val accountsModel = GEAccountsComboBoxModel(accounts, defaultAccount ?: accounts.firstOrNull())

  val fileName: String?
    get() = fileNameField?.text

  val description: String
    get() = descriptionField.text

  val isSecret: Boolean
    get() = secretCheckBox.isSelected

  val isOpenInBrowser: Boolean
    get() = browserCheckBox.isSelected

  val isCopyURL: Boolean
    get() = copyLinkCheckBox.isSelected

//  val account: GiteeAccount
//    get() = accountSelector.selectedItem as GiteeAccount
  val account: GiteeAccount?
    get() = accountsModel.selected

  init {
    title = "Create Gist"
    init()
  }

  override fun createCenterPanel() = panel {
    fileNameField?.let {
      row("Filename:") {
        it(pushX, growY)
      }
    }
    row("Description:") {
      scrollPane(descriptionField)
    }
    row("") {
      cell {
        secretCheckBox()
        browserCheckBox()
        copyLinkCheckBox()
      }
    }
//    if (accountSelector.isEnabled) {
//      row("Create for:") {
//        accountSelector(pushX, growX)
//      }
//    }
    if (accountsModel.size != 1) {
      row(message("create.gist.dialog.create.for.field")) {
        accountSelector(accountsModel)
      }
//      row(message("create.gist.dialog.create.for.field")) {
//        comboBox(accountsModel)
//          .horizontalAlign(HorizontalAlign.FILL)
//          .validationOnApply { if (accountsModel.selected == null) error(message("dialog.message.account.cannot.be.empty")) else null }
//          .resizableColumn()
//
//        if (accountsModel.size == 0) {
//          cell(GEAccountsHost.createAddAccountLink())
//        }
//      }
    }
  }

  override fun getHelpId(): String = "gitee.create.gist.dialog"
  override fun getDimensionServiceKey(): String = "Gitee.CreateGistDialog"
  override fun getPreferredFocusedComponent(): JComponent = descriptionField

  override fun getData(dataId: String): Any? =
    if (GEAccountsHost.KEY.`is`(dataId)) accountsModel
    else null
}
