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
package com.gitee.authentication.accounts

import com.gitee.util.GiteeNotifications
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
 *
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/authentication/accounts/GithubProjectDefaultAccountHolder.kt
 * @author JetBrains s.r.o.
 */
@State(name = "GiteeDefaultAccount", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
internal class GiteeProjectDefaultAccountHolder(private val project: Project,
                                                private val accountManager: GiteeAccountManager) : PersistentStateComponent<AccountState> {
  var account: GiteeAccount? = null

  init {
    ApplicationManager.getApplication()
      .messageBus
      .connect(project)
      .subscribe(GiteeAccountManager.ACCOUNT_REMOVED_TOPIC, object : AccountRemovedListener {
        override fun accountRemoved(removedAccount: GiteeAccount) {
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

  private fun findAccountById(id: String): GiteeAccount? {
    val account = accountManager.accounts.find { it.id == id }
    if (account == null) runInEdt {
      GiteeNotifications.showWarning(project, "Missing Default Gitee Account", "",
        GiteeNotifications.getConfigureAction(project))
    }
    return account
  }
}

internal class AccountState {
  var defaultAccountId: String? = null
}

