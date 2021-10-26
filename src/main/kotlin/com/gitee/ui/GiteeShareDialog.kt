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
import com.gitee.ui.util.DialogValidationUtils.RecordUniqueValidator
import com.gitee.ui.util.DialogValidationUtils.notBlank
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.NlsSafe
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextField
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
class GiteeShareDialog(project: Project,
                       accounts: Set<GiteeAccount>,
                       defaultAccount: GiteeAccount?,
                       existingRemotes: Set<String>,
                       private val accountInformationSupplier: (GiteeAccount, Component) -> Pair<Boolean, Set<String>>)
  : DialogWrapper(project), DataProvider {

  private val GITEE_REPO_PATTERN = Pattern.compile("[a-zA-Z0-9_.-]+")

  private val repositoryTextField = JBTextField(project.name)
  private val privateCheckBox = JBCheckBox(message("share.dialog.private"), false)

  @NlsSafe
  private val remoteName = if (existingRemotes.isEmpty()) "origin" else "github"
  private val remoteTextField = JBTextField(remoteName)

  private val descriptionTextArea = JTextArea()

  private val existingRepoValidator = RecordUniqueValidator(repositoryTextField, message("share.error.repo.with.selected.name.exists"))
  private val existingRemoteValidator = RecordUniqueValidator(remoteTextField, message("share.error.remote.with.selected.name.exists"))
    .apply { records = existingRemotes }

  private var accountInformationLoadingError: ValidationInfo? = null

  //  private val accountSelector = GiteeAccountCombobox(accounts, defaultAccount) { switchAccount(it) }
  private val accountsModel = GEAccountsComboBoxModel(accounts, defaultAccount ?: accounts.firstOrNull())

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
//      accountInformationLoadingError = if (e is ProcessCanceledException) {
//        ValidationInfo("Cannot load information for $account:\nProcess cancelled")
//      }
//      else ValidationInfo("Cannot load information for $account:\n$e")

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

//  override fun createCenterPanel(): JComponent? {
//    val descriptionPane = JBScrollPane(descriptionTextArea).apply {
//      minimumSize = JBDimension(150, 50)
//      preferredSize = JBDimension(150, 50)
//    }
//
//    val repository = JBBox.createHorizontalBox()
//    repository.add(repositoryTextField)
//    repository.add(JBBox.createRigidArea(JBUI.size(UIUtil.DEFAULT_HGAP, 0)))
//    repository.add(privateCheckBox)
//    repository.add(JBBox.createRigidArea(JBUI.size(5, 0)))
//
//    return grid().resize()
//      .add(panel(repository).withLabel("Repository name:"))
//      .add(panel(remoteTextField).withLabel("Remote:"))
//      .add(panel(descriptionPane).withLabel("Description:").anchorLabelOn(UI.Anchor.Top).resizeY(true))
//      .apply {
//        if (accountSelector.isEnabled) add(panel(accountSelector).withLabel("Share by:").resizeX(false))
//      }
//      .createPanel()
//  }
  override fun createCenterPanel() = com.intellij.ui.layout.panel {
    row(message("share.dialog.repo.name")) {
      cell {
        repositoryTextField(growX, pushX).withValidationOnApply { validateRepository() }
        privateCheckBox()
      }
    }
    row(message("share.dialog.remote")) {
      remoteTextField(growX, pushX).withValidationOnApply { validateRemote() }
    }
    row(message("share.dialog.description")) {
      scrollPane(descriptionTextArea)
    }
    if (accountsModel.size != 1) {
      row(message("share.dialog.share.by")) {
        accountSelector(accountsModel) { switchAccount(getAccount()) }
      }
    }
  }


//  override fun doValidateAll(): List<ValidationInfo> {
//    val repositoryNamePatternMatchValidator: Validator = {
//      if (!GITEE_REPO_PATTERN.matcher(repositoryTextField.text).matches()) ValidationInfo(
//        "Invalid repository name. Name should consist of letters, numbers, dashes, dots and underscores",
//        repositoryTextField)
//      else null
//    }
//
//    return listOf({ accountInformationLoadingError },
//                  chain({ notBlank(repositoryTextField, "No repository name selected") },
//                        repositoryNamePatternMatchValidator,
//                        existingRepoValidator),
//                  chain({ notBlank(remoteTextField, "No remote name selected") },
//                        existingRemoteValidator)
//    ).mapNotNull { it() }
//  }
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

  override fun getData(dataId: String): Any? =
    if (GEAccountsHost.KEY.`is`(dataId)) accountsModel
    else null

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
