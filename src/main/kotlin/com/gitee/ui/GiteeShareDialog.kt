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
import com.gitee.authentication.ui.GiteeAccountCombobox
import com.gitee.ui.util.DialogValidationUtils.RecordUniqueValidator
import com.gitee.ui.util.DialogValidationUtils.chain
import com.gitee.ui.util.DialogValidationUtils.notBlank
import com.gitee.ui.util.Validator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.components.JBBox
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UI
import com.intellij.util.ui.UI.PanelFactory.grid
import com.intellij.util.ui.UI.PanelFactory.panel
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.dialog.DialogUtils
import org.jetbrains.annotations.TestOnly
import java.awt.Component
import java.util.regex.Pattern
import javax.swing.JComponent
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
  : DialogWrapper(project) {

  private val GITEE_REPO_PATTERN = Pattern.compile("[a-zA-Z0-9_.-]+")

  private val repositoryTextField = JBTextField(project.name)
  private val privateCheckBox = JBCheckBox("Private", false)
  private val remoteTextField = JBTextField(if (existingRemotes.isEmpty()) "origin" else "gitee")
  private val descriptionTextArea = JTextArea()
  private val accountSelector = GiteeAccountCombobox(accounts, defaultAccount) { switchAccount(it) }
  private val existingRepoValidator = RecordUniqueValidator(repositoryTextField, "Repository with selected name already exists")
  private val existingRemoteValidator = RecordUniqueValidator(remoteTextField, "Remote with selected name already exists")
    .apply { records = existingRemotes }
  private var accountInformationLoadingError: ValidationInfo? = null

  init {
    title = "Share Project On Gitee"
    setOKButtonText("Share")
    init()
    DialogUtils.invokeLaterAfterDialogShown(this) { switchAccount(accountSelector.selectedItem as GiteeAccount) }
  }

  private fun switchAccount(account: GiteeAccount) {
    try {
      accountInformationLoadingError = null
      accountInformationSupplier(account, window).let {
        privateCheckBox.isEnabled = it.first
        if (!it.first) privateCheckBox.toolTipText = "Your account doesn't support private repositories"
        else privateCheckBox.toolTipText = null
        existingRepoValidator.records = it.second
      }
    }
    catch (e: Exception) {
      accountInformationLoadingError = if (e is ProcessCanceledException) {
        ValidationInfo("Cannot load information for $account:\nProcess cancelled")
      }
      else ValidationInfo("Cannot load information for $account:\n$e")
      privateCheckBox.isEnabled = false
      privateCheckBox.toolTipText = null
      existingRepoValidator.records = emptySet()
      startTrackingValidation()
    }
  }

  override fun createCenterPanel(): JComponent? {
    val descriptionPane = JBScrollPane(descriptionTextArea).apply {
      minimumSize = JBDimension(150, 50)
      preferredSize = JBDimension(150, 50)
    }

    val repository = JBBox.createHorizontalBox()
    repository.add(repositoryTextField)
    repository.add(JBBox.createRigidArea(JBUI.size(UIUtil.DEFAULT_HGAP, 0)))
    repository.add(privateCheckBox)
    repository.add(JBBox.createRigidArea(JBUI.size(5, 0)))

    return grid().resize()
      .add(panel(repository).withLabel("Repository name:"))
      .add(panel(remoteTextField).withLabel("Remote:"))
      .add(panel(descriptionPane).withLabel("Description:").anchorLabelOn(UI.Anchor.Top).resizeY(true))
      .apply {
        if (accountSelector.isEnabled) add(panel(accountSelector).withLabel("Share by:").resizeX(false))
      }
      .createPanel()
  }

  override fun doValidateAll(): List<ValidationInfo> {
    val repositoryNamePatternMatchValidator: Validator = {
      if (!GITEE_REPO_PATTERN.matcher(repositoryTextField.text).matches()) ValidationInfo(
        "Invalid repository name. Name should consist of letters, numbers, dashes, dots and underscores",
        repositoryTextField)
      else null
    }

    return listOf({ accountInformationLoadingError },
                  chain({ notBlank(repositoryTextField, "No repository name selected") },
                        repositoryNamePatternMatchValidator,
                        existingRepoValidator),
                  chain({ notBlank(remoteTextField, "No remote name selected") },
                        existingRemoteValidator)
    ).mapNotNull { it() }
  }

  override fun getHelpId(): String = "gitee.share"
  override fun getDimensionServiceKey(): String = "Gitee.ShareDialog"
  override fun getPreferredFocusedComponent(): JBTextField = repositoryTextField

  fun getRepositoryName(): String = repositoryTextField.text
  fun getRemoteName(): String = remoteTextField.text
  fun isPrivate(): Boolean = privateCheckBox.isSelected
  fun getDescription(): String = descriptionTextArea.text
  fun getAccount(): GiteeAccount = accountSelector.selectedItem as GiteeAccount

  @TestOnly
  fun testSetRepositoryName(name: String) {
    repositoryTextField.text = name
  }
}
