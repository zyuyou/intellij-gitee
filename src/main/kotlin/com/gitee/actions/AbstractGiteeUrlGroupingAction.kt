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
package com.gitee.actions

import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.ui.GiteeChooseAccountDialog
import com.gitee.util.GiteeGitHelper
import com.gitee.util.GiteeUrlUtil
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import git4idea.DialogManager
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import javax.swing.Icon

/**
 * Visible and enabled if there's at least one possible gitee remote url ([GiteeGitHelper]).
 * If there's only one url - it will be used for action, otherwise child actions will be created for each url.
 *
 * If it is not possible to automatically determine suitable account, [GiteeChooseAccountDialog] dialog will be shown.
 */
abstract class AbstractGiteeUrlGroupingAction(text: String?, description: String?, icon: Icon?)
  : ActionGroup(text, description, icon), DumbAware {

  final override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = isEnabledAndVisible(e)
  }

  protected open fun isEnabledAndVisible(e: AnActionEvent): Boolean {
    val project = e.getData(CommonDataKeys.PROJECT)
    if (project == null || project.isDefault) return false

    return service<GiteeGitHelper>().havePossibleRemotes(project)
  }

  final override fun getChildren(e: AnActionEvent?): Array<AnAction> {
    val project = e?.getData(CommonDataKeys.PROJECT) ?: return AnAction.EMPTY_ARRAY

    val coordinates = service<GiteeGitHelper>().getPossibleRemoteUrlCoordinates(project)

    return if (coordinates.size > 1) {
      coordinates.map {
        object : DumbAwareAction(GiteeUrlUtil.removeProtocolPrefix(it.url)) {
          override fun actionPerformed(e: AnActionEvent) {
            actionPerformed(e, project, it.repository, it.remote, it.url)
          }
        }
      }.toTypedArray()
    }
    else AnAction.EMPTY_ARRAY
  }

  final override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT) ?: return

    val coordinates = service<GiteeGitHelper>().getPossibleRemoteUrlCoordinates(project)
    coordinates.singleOrNull()?.let { actionPerformed(e, project, it.repository, it.remote, it.url) }
  }

  final override fun canBePerformed(context: DataContext): Boolean {
    val project = context.getData(CommonDataKeys.PROJECT) ?: return false

    val coordinates = service<GiteeGitHelper>().getPossibleRemoteUrlCoordinates(project)
    return coordinates.size == 1
  }

  final override fun isPopup(): Boolean = true
  final override fun disableIfNoVisibleChildren(): Boolean = false

  private fun actionPerformed(e: AnActionEvent, project: Project, repository: GitRepository, remote: GitRemote, remoteUrl: String) {
//    if (!service<GiteeAccountsMigrationHelper>().migrate(project)) return
    val account = getAccount(project, remoteUrl) ?: return
    actionPerformed(e, project, repository, remote, remoteUrl, account)
  }

  private fun getAccount(project: Project, remoteUrl: String): GiteeAccount? {
    val authenticationManager = service<GiteeAuthenticationManager>()
    val accounts = authenticationManager.getAccounts().filter { it.server.matches(remoteUrl) }
    //only possible when remote is on gitee.com
    if (accounts.isEmpty()) {
      if (!GiteeServerPath.DEFAULT_SERVER.matches(remoteUrl))
        throw IllegalArgumentException("Remote $remoteUrl does not match ${GiteeServerPath.DEFAULT_SERVER}")
      return authenticationManager.requestNewAccountForServer(GiteeServerPath.DEFAULT_SERVER, project)
    }

    return accounts.singleOrNull()
           ?: accounts.find { it == authenticationManager.getDefaultAccount(project) }
           ?: chooseAccount(project, authenticationManager, remoteUrl, accounts)
  }

  private fun chooseAccount(project: Project, authenticationManager: GiteeAuthenticationManager,
                            remoteUrl: String, accounts: List<GiteeAccount>): GiteeAccount? {
    val dialog = GiteeChooseAccountDialog(project,
                                           null,
                                           accounts,
                                           "Choose Gitee account for: $remoteUrl",
                                           false,
                                           true)
    DialogManager.show(dialog)
    if (!dialog.isOK) return null
    val account = dialog.account
    if (dialog.setDefault) authenticationManager.setDefaultAccount(project, account)
    return account
  }

  protected abstract fun actionPerformed(e: AnActionEvent,
                                         project: Project,
                                         repository: GitRepository,
                                         remote: GitRemote,
                                         remoteUrl: String,
                                         account: GiteeAccount)
}