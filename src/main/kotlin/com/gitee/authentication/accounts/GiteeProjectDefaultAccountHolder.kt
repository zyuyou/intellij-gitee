// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.accounts

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.project.Project

/**
 * Handles default Gitee account for project
 *
 * TODO: auto-detection
 */
@State(name = "GiteeDefaultAccount", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
internal class GiteeProjectDefaultAccountHolder(private val project: Project,
                                                private val accountManager: GiteeAccountManager) : PersistentStateComponent<AccountState> {
  var account: com.gitee.authentication.accounts.GiteeAccount? = null

  init {
    ApplicationManager.getApplication()
      .messageBus
      .connect(project)
      .subscribe(GiteeAccountManager.ACCOUNT_REMOVED_TOPIC, object : AccountRemovedListener {
        override fun accountRemoved(removedAccount: com.gitee.authentication.accounts.GiteeAccount) {
          if (account == removedAccount) account = null
        }
      })
  }

  override fun getState(): AccountState {
    return AccountState().apply { defaultAccountId = account?.id }
  }

  override fun loadState(state: AccountState) {
    account = state.defaultAccountId?.let(::findAccountById)
  }

  private fun findAccountById(id: String): com.gitee.authentication.accounts.GiteeAccount? {
    val account = accountManager.accounts.find { it.id == id }
    if (account == null) runInEdt {
      com.gitee.util.GiteeNotifications.showWarning(project, "Missing Default Gitee Account", "",
        com.gitee.util.GiteeNotifications.getConfigureAction(project))
    }
    return account
  }
}

internal class AccountState {
  var defaultAccountId: String? = null
}

