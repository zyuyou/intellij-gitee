// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.actions

import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.ui.GiteeChooseAccountDialog
import com.gitee.util.GiteeGitHelper
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Pair
import com.intellij.openapi.vfs.VirtualFile
import git4idea.DialogManager
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import javax.swing.Icon

abstract class LegacySingleAccountActionGroup(text: String?, description: String?, icon: Icon?) : DumbAwareAction(text, description, icon) {
  override fun update(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT)
    val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
    if (project == null || project.isDefault) {
      e.presentation.isEnabledAndVisible = false
      return
    }

    val gitRepository = GiteeGitHelper.findGitRepository(project, file)
    if (gitRepository == null) {
      e.presentation.isEnabledAndVisible = false
      return
    }

    if (service<GiteeGitHelper>().getPossibleRepositories(gitRepository).isEmpty()) {
      e.presentation.isEnabledAndVisible = false
      return
    }

    e.presentation.isEnabledAndVisible = true
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT)
    val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
    if (project == null || project.isDefault) return

    val gitRepository = GiteeGitHelper.findGitRepository(project, file) ?: return

    gitRepository.update()

    val authenticationManager = service<GiteeAuthenticationManager>()

//    if (!service<GiteeAccountsMigrationHelper>().migrate(project)) return

    val accounts = getAccountsForRemotes(authenticationManager, project, gitRepository)

    // can happen if migration was cancelled
    if (accounts.isEmpty()) return

    val account = if (accounts.size == 1) {
      accounts.first()
    } else {
      val dialog = GiteeChooseAccountDialog(project, null, accounts,
        "Default account is not configured for this project. Choose Gitee account:",
        true,
        !project.isDefault,
        "Choose Gitee Account",
        "Choose")

      DialogManager.show(dialog)
      if (!dialog.isOK) return
      val account = dialog.account
      if (dialog.setDefault) authenticationManager.setDefaultAccount(project, account)

      account
    }

    actionPerformed(project, file, gitRepository, account)
  }

  abstract fun actionPerformed(project: Project, file: VirtualFile?, gitRepository: GitRepository, account: GiteeAccount)

  private fun getAccountsForRemotes(authenticationManager: GiteeAuthenticationManager,
                                    project: Project, repository: GitRepository): List<GiteeAccount> {

    if (!authenticationManager.ensureHasAccounts(project)) return emptyList()

    val defaultAccount = authenticationManager.getDefaultAccount(project)

    return if (defaultAccount != null && getRemote(defaultAccount.server, repository) != null) {
      listOf(defaultAccount)
    } else {
      authenticationManager.getAccounts().filter { getRemote(it.server, repository) != null }
    }
  }

  protected abstract fun getRemote(server: GiteeServerPath, repository: GitRepository): Pair<GitRemote, String>?
}