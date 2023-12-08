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

import com.gitee.authentication.accounts.GEAccountManager
import com.gitee.authentication.accounts.GiteeProjectDefaultAccountHolder
import com.gitee.authentication.ui.GEAccountsDetailsProvider
import com.gitee.authentication.ui.GEAccountsListModel
import com.gitee.authentication.ui.GEAccountsPanelActionsController
import com.gitee.i18n.GiteeBundle
import com.gitee.util.GiteeSettings
import com.gitee.util.GiteeUtil
import com.intellij.collaboration.async.DisposingMainScope
import com.intellij.collaboration.auth.ui.AccountsPanelFactory
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.components.service
import com.intellij.openapi.options.BoundConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.*
import kotlinx.coroutines.plus

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/ui/GithubSettingsConfigurable.kt
 * @author JetBrains s.r.o.
 */
internal class GiteeSettingsConfigurable internal constructor(private val project: Project)
  : BoundConfigurable(GiteeUtil.SERVICE_DISPLAY_NAME, "settings.gitee") {

  override fun createPanel(): DialogPanel {
    val defaultAccountHolder = project.service<GiteeProjectDefaultAccountHolder>()
    val accountManager = service<GEAccountManager>()
    val settings = GiteeSettings.getInstance()

    val scope = DisposingMainScope(disposable!!) + ModalityState.any().asContextElement()
    val accountsModel = GEAccountsListModel()
    val detailsProvider = GEAccountsDetailsProvider(scope, accountManager, accountsModel)

    val panelFactory = AccountsPanelFactory(scope, accountManager, defaultAccountHolder, accountsModel)
    val actionsController = GEAccountsPanelActionsController(project, accountsModel)

    return panel {
      row {
        panelFactory.accountsPanelCell(this, detailsProvider, actionsController)
          .align(Align.FILL)
      }.resizableRow()

      row {
        checkBox(GiteeBundle.message("settings.clone.ssh"))
          .bindSelected(settings::isCloneGitUsingSsh, settings::setCloneGitUsingSsh)
      }
      row(GiteeBundle.message("settings.timeout")) {
        intTextField(range = 0..60)
          .columns(2)
          .bindIntText({ settings.connectionTimeout / 1000 }, { settings.connectionTimeout = it * 1000 })
          .gap(RightGap.SMALL)
        @Suppress("DialogTitleCapitalization")
        label(GiteeBundle.message("settings.timeout.seconds"))
      }
    }
  }
}