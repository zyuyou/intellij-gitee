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
import com.gitee.ui.util.DialogValidationUtils.RecordUniqueValidator
import com.gitee.ui.util.DialogValidationUtils.notBlank
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.NlsSafe
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.dialog.DialogUtils
import org.jetbrains.annotations.TestOnly
import java.awt.Component
import java.util.regex.Pattern
import javax.swing.JTextArea


/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/ui/GithubShareDialog.kt
 * @author JetBrains s.r.o.
 */
class GiteeShareDialog(private val project: Project,
                       existingRemotes: Set<String>,
                       private val accountInformationSupplier: (GiteeAccount, Component) -> Pair<Boolean, Set<String>>)
  : DialogWrapper(project) {

  private val GITEE_REPO_PATTERN = Pattern.compile("[a-zA-Z0-9_.-]+")

  private val repositoryTextField = JBTextField(project.name)
  private val privateCheckBox = JBCheckBox(message("share.dialog.private"), false)

  @NlsSafe
  private val remoteName = if (existingRemotes.isEmpty()) "origin" else "gitee"
  private val remoteTextField = JBTextField(remoteName)

  private val descriptionTextArea = JTextArea()

  private val existingRepoValidator = RecordUniqueValidator(repositoryTextField, message("share.error.repo.with.selected.name.exists"))
  private val existingRemoteValidator = RecordUniqueValidator(remoteTextField, message("share.error.remote.with.selected.name.exists"))
    .apply { records = existingRemotes }

  private var accountInformationLoadingError: ValidationInfo? = null

  private val accounts = GEAccountsUtil.accounts

//  private val accountsModel = GEAccountsComboBoxModel(
//    accounts,
//    GEAccountsUtil.getDefaultAccount(project) ?: accounts.firstOrNull()
//  )
  private val accountsModel = CollectionComboBoxModel(
    accounts.toMutableList(),
    GEAccountsUtil.getDefaultAccount(project) ?: accounts.firstOrNull()
  )

  init {
    title = message("share.on.gitee")
    setOKButtonText(message("share.button"))
    init()
    DialogUtils.invokeLaterAfterDialogShown(this) { switchAccount(getAccount()) }
  }

  private fun switchAccount(account: GiteeAccount?) {
    if (account == null) return

    try {
      accountInformationLoadingError = null
      accountInformationSupplier(account, window).let {
        privateCheckBox.isEnabled = it.first
        if (!it.first) privateCheckBox.toolTipText = message("share.error.private.repos.not.supported")
        else privateCheckBox.toolTipText = null
        existingRepoValidator.records = it.second
      }
    }
    catch (e: Exception) {
      val errorText = message("share.dialog.account.info.load.error.prefix", account) +
        if (e is ProcessCanceledException) message("share.dialog.account.info.load.process.canceled")
        else e.message
      accountInformationLoadingError = ValidationInfo(errorText)

      privateCheckBox.isEnabled = false
      privateCheckBox.toolTipText = null
      existingRepoValidator.records = emptySet()
      startTrackingValidation()
    }
  }

  override fun createCenterPanel() = panel {
    row(message("share.dialog.repo.name")) {
      cell(repositoryTextField)
        .align(AlignX.FILL)
        .validationOnApply { validateRepository() }
        .resizableColumn()
      cell(privateCheckBox)
    }
    row(message("share.dialog.remote")) {
      cell(remoteTextField)
        .align(AlignX.FILL)
        .validationOnApply { validateRemote() }
    }
    row(message("share.dialog.description")) {
      label(message("share.dialog.description"))
        .align(AlignY.TOP)
      scrollCell(descriptionTextArea)
        .align(Align.FILL)
    }.layout(RowLayout.LABEL_ALIGNED).resizableRow()

    if (accountsModel.size != 1) {
      row(message("share.dialog.share.by")) {
        comboBox(accountsModel)
          .align(AlignX.FILL)
          .validationOnApply { if (accountsModel.selected == null) kotlin.error(message("dialog.message.account.cannot.be.empty")) else null }
          .applyToComponent { addActionListener { switchAccount(getAccount()) } }
          .resizableColumn()

        if (accountsModel.size == 0) {
          cell(GEAccountsUtil.createAddAccountLink(project, accountsModel))
        }
      }
    }
  }.apply {
    preferredSize = JBUI.size(500, 250)
  }

  override fun doValidateAll(): List<ValidationInfo> {
    val uiErrors = super.doValidateAll()
    val loadingError = accountInformationLoadingError

    return if (loadingError != null) uiErrors + loadingError else uiErrors
  }

  private fun validateRepository(): ValidationInfo? =
    notBlank(repositoryTextField, message("share.validation.no.repo.name"))
      ?: validateRepositoryName()
      ?: existingRepoValidator()

  private fun validateRepositoryName(): ValidationInfo? =
    if (GITEE_REPO_PATTERN.matcher(repositoryTextField.text).matches()) null
    else ValidationInfo(message("share.validation.invalid.repo.name"), repositoryTextField)

  private fun validateRemote(): ValidationInfo? =
    notBlank(remoteTextField, message("share.validation.no.remote.name"))
      ?: existingRemoteValidator()

  override fun getHelpId(): String = "gitee.share"
  override fun getDimensionServiceKey(): String = "Gitee.ShareDialog"
  override fun getPreferredFocusedComponent(): JBTextField = repositoryTextField

  fun getRepositoryName(): String = repositoryTextField.text
  fun getRemoteName(): String = remoteTextField.text
  fun isPrivate(): Boolean = privateCheckBox.isSelected
  fun getDescription(): String = descriptionTextArea.text
//  fun getAccount(): GiteeAccount = accountSelector.selectedItem as GiteeAccount
  fun getAccount(): GiteeAccount? = accountsModel.selected

  @TestOnly
  fun testSetRepositoryName(name: String) {
    repositoryTextField.text = name
  }
}
