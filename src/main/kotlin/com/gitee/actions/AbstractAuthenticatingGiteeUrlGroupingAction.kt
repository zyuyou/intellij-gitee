// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.actions

//import com.gitee.util.GiteeAccountsMigrationHelper
import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.ui.GiteeChooseAccountDialog
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import git4idea.DialogManager
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import javax.swing.Icon

/**
 * If it is not possible to automatically determine suitable account, [GithubChooseAccountDialog] dialog will be shown.
 */
abstract class AbstractAuthenticatingGiteeUrlGroupingAction(text: String?, description: String?, icon: Icon?)
  : AbstractGiteeUrlGroupingAction(text, description, icon) {

  override fun actionPerformed(e: AnActionEvent, project: Project, repository: GitRepository, remote: GitRemote, remoteUrl: String) {
//    if (!service<GiteeAccountsMigrationHelper>().migrate(project)) return
    val account = getAccount(project, remoteUrl) ?: return
    actionPerformed(e, project, repository, remote, remoteUrl, account)
  }

  private fun getAccount(project: Project, remoteUrl: String): GiteeAccount? {
    val authenticationManager = service<GiteeAuthenticationManager>()
    val accounts = authenticationManager.getAccounts().filter { it.server.matches(remoteUrl) }
    //only possible when remote is on github.com
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
                                           "Choose GitHub account for: $remoteUrl",
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