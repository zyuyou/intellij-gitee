// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.ui

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.authentication.accounts.*
import com.gitee.util.GiteeSettings
import com.gitee.util.GiteeUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableBase
import com.intellij.openapi.project.Project

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
