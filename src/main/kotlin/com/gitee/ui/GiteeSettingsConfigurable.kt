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
package com.gitee.ui

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.authentication.accounts.*
import com.gitee.util.GiteeSettings
import com.gitee.util.GiteeUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableBase
import com.intellij.openapi.project.Project

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/ui/util/GithubSettingsConfigurable.kt
 * @author JetBrains s.r.o.
 */
class GiteeSettingsConfigurable internal constructor(private val project: Project,
                                                     private val settings: GiteeSettings,
                                                     private val accountManager: GiteeAccountManager,
                                                     private val defaultAccountHolder: GiteeProjectDefaultAccountHolder,
                                                     private val executorFactory: GiteeApiRequestExecutor.Factory,
                                                     private val accountInformationProvider: GiteeAccountInformationProvider) :
  ConfigurableBase<GiteeSettingsPanel, GiteeSettingsConfigurable.GiteeSettingsHolder>(
    "settings.gitee",
    GiteeUtil.SERVICE_DISPLAY_NAME,
    "settings.gitee"
  ), Configurable.NoMargin {

  init {
    ApplicationManager.getApplication()
      .messageBus
      .connect(project)
      .subscribe(GiteeAccountManager.ACCOUNT_TOKEN_CHANGED_TOPIC, object : AccountTokenChangedListener {
        override fun tokenChanged(account: GiteeAccount) {
          if (!isModified) reset()
        }
      })
  }

  override fun getSettings(): GiteeSettingsHolder {
    return GiteeSettingsHolder(settings, accountManager, defaultAccountHolder)
  }

  override fun createUi(): GiteeSettingsPanel {
    return GiteeSettingsPanel(project, executorFactory, accountInformationProvider)
  }

  inner class GiteeSettingsHolder internal constructor(val application: GiteeSettings,
                                                       val applicationAccounts: GiteeAccountManager,
                                                       val projectAccount: GiteeProjectDefaultAccountHolder)
}
