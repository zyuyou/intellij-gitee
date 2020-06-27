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

import com.gitee.GiteeBundle
import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.authentication.accounts.AccountTokenChangedListener
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.accounts.GiteeAccountManager
import com.gitee.authentication.accounts.GiteeProjectDefaultAccountHolder
import com.gitee.authentication.ui.GiteeAccountsPanel
import com.gitee.util.CachingGiteeUserAvatarLoader
import com.gitee.util.GiteeImageResizer
import com.gitee.util.GiteeSettings
import com.gitee.util.GiteeUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.util.Disposer
import com.intellij.ui.layout.panel

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/ui/GithubSettingsConfigurable.kt
 * @author JetBrains s.r.o.
 */
//class GiteeSettingsConfigurable internal constructor(private val project: Project,
//                                                     private val settings: GiteeSettings,
//                                                     private val accountManager: GiteeAccountManager,
//                                                     private val defaultAccountHolder: GiteeProjectDefaultAccountHolder,
//                                                     private val executorFactory: GiteeApiRequestExecutor.Factory,
//                                                     private val avatarLoader: CachingGiteeUserAvatarLoader,
//                                                     private val imageResizer: GiteeImageResizer) :
//  ConfigurableBase<GiteeSettingsPanel, GiteeSettingsConfigurable.GiteeSettingsHolder>(
//    "settings.gitee",
//    GiteeUtil.SERVICE_DISPLAY_NAME,
//    "settings.gitee"
//  ), Configurable.NoMargin {
//
//  init {
//    ApplicationManager.getApplication()
//      .messageBus
//      .connect(project)
//      .subscribe(GiteeAccountManager.ACCOUNT_TOKEN_CHANGED_TOPIC, object : AccountTokenChangedListener {
//        override fun tokenChanged(account: GiteeAccount) {
//          if (!isModified) reset()
//        }
//      })
//  }
//
//  override fun getSettings(): GiteeSettingsHolder {
//    return GiteeSettingsHolder(settings, accountManager, defaultAccountHolder)
//  }
//
//  override fun createUi(): GiteeSettingsPanel {
//    return GiteeSettingsPanel(project, executorFactory, avatarLoader, imageResizer)
//  }
//
//  inner class GiteeSettingsHolder internal constructor(val application: GiteeSettings,
//                                                       val applicationAccounts: GiteeAccountManager,
//                                                       val projectAccount: GiteeProjectDefaultAccountHolder)
//}
internal class GiteeSettingsConfigurable internal constructor(private val project: Project) : BoundConfigurable(GiteeUtil.SERVICE_DISPLAY_NAME, "settings.github") {
  override fun createPanel(): DialogPanel {
    val defaultAccountHolder = project.service<GiteeProjectDefaultAccountHolder>()
    val accountManager = service<GiteeAccountManager>()
    val settings = GiteeSettings.getInstance()
    return panel {
      row {
        val accountsPanel = GiteeAccountsPanel(project, GiteeApiRequestExecutor.Factory.getInstance(), CachingGiteeUserAvatarLoader.getInstance(), GiteeImageResizer.getInstance()).apply {
          Disposer.register(disposable!!, this)
        }
        component(accountsPanel)
          .onIsModified { accountsPanel.isModified(accountManager.accounts, defaultAccountHolder.account) }
          .onReset {
            val accountsMap = accountManager.accounts.associateWith { accountManager.getTokensForAccount(it) }
            accountsPanel.setAccounts(accountsMap, defaultAccountHolder.account)
            accountsPanel.clearNewTokens()
            accountsPanel.loadExistingAccountsDetails()
          }
          .onApply {
            val (accountsTokenMap, defaultAccount) = accountsPanel.getAccounts()
            accountManager.accounts = accountsTokenMap.keys
            accountsTokenMap.filterValues { it != null }
              .mapValues { "${it.value?.first}&${it.value?.second}" }
              .forEach(accountManager::updateAccountToken)
            defaultAccountHolder.account = defaultAccount
            accountsPanel.clearNewTokens()
          }

        ApplicationManager.getApplication().messageBus.connect(disposable!!)
          .subscribe(GiteeAccountManager.ACCOUNT_TOKEN_CHANGED_TOPIC,
            object : AccountTokenChangedListener {
              override fun tokenChanged(account: GiteeAccount) {
                if (!isModified) reset()
              }
            })
      }
      row {
        checkBox(GiteeBundle.message("settings.clone.ssh"), settings::isCloneGitUsingSsh, settings::setCloneGitUsingSsh)
      }
      row {
        cell {
          label(GiteeBundle.message("settings.timeout"))
          intTextField({ settings.connectionTimeout / 1000 }, { settings.connectionTimeout = it * 1000 }, columns = 2, range = 0..60)
          label(GiteeBundle.message("settings.timeout.seconds"))
        }
      }
    }
  }
}