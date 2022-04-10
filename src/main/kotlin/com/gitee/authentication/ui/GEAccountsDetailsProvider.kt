// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.ui

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.data.GiteeUserDetailed
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GEAccountManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.util.GESecurityUtil
import com.gitee.i18n.GiteeBundle
import com.gitee.icons.GiteeIcons
import com.gitee.util.CachingGEUserAvatarLoader
import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.collaboration.async.CompletableFutureUtil.successOnEdt
import com.intellij.collaboration.auth.ui.LoadingAccountsDetailsProvider
import com.intellij.collaboration.util.ProgressIndicatorsProvider
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.IconUtil
import java.util.concurrent.CompletableFuture

internal class GEAccountsDetailsProvider(progressIndicatorsProvider: ProgressIndicatorsProvider,
                                         private val accountManager: GEAccountManager,
                                         private val accountsModel: GEAccountsListModel)
  : LoadingAccountsDetailsProvider<GiteeAccount, GiteeUserDetailed>(progressIndicatorsProvider) {

  override val defaultIcon = IconUtil.resizeSquared(GiteeIcons.DefaultAvatar, 40)

  override fun scheduleLoad(account: GiteeAccount,
                            indicator: ProgressIndicator): CompletableFuture<DetailsLoadingResult<GiteeUserDetailed>> {

    val credentials = accountsModel.newCredentials.getOrElse(account) {
      accountManager.findCredentials(account)
    } ?: return CompletableFuture.completedFuture(noCredentials())

    val executor = service<GiteeApiRequestExecutor.Factory>().create(credentials) {
        newCredentials -> GiteeAuthenticationManager.getInstance().updateAccountCredentials(account, newCredentials)
    }

    return ProgressManager.getInstance().submitIOTask(EmptyProgressIndicator()) {
      val (details, _) = GESecurityUtil.loadCurrentUserWithScopes(executor, it, account.server)
//      if (!GESecurityUtil.isEnoughScopes(scopes.orEmpty())) return@submitIOTask noScopes()
      val image = details.avatarUrl?.let { url -> CachingGEUserAvatarLoader.getInstance().requestAvatar(executor, url).join() }
      DetailsLoadingResult<GiteeUserDetailed>(details, image, null, false)
    }.successOnEdt(ModalityState.any()) {
      accountsModel.accountsListModel.contentsChanged(account)
      it
    }
  }

  private fun noCredentials() = DetailsLoadingResult<GiteeUserDetailed>(null, null, GiteeBundle.message("account.credentials.missing"), true)
}