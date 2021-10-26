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

    val token = accountsModel.newCredentials.getOrElse(account) {
      accountManager.findCredentials(account)
    } ?: return CompletableFuture.completedFuture(noToken())

    val tokens = token.split("&").let {
      tokenList ->
        if (tokenList.size == 1) Pair(tokenList[0], "") else Pair(tokenList[0], tokenList[1])
    }

    val executor = service<GiteeApiRequestExecutor.Factory>().create(tokens) {
      newTokens -> GiteeAuthenticationManager.getInstance().updateAccountToken(account, "${newTokens.first}&${newTokens.second}")
    }

    return ProgressManager.getInstance().submitIOTask(EmptyProgressIndicator()) {
      val (details, scopes) = GESecurityUtil.loadCurrentUserWithScopes(executor, it, account.server)
//      if (!GESecurityUtil.isEnoughScopes(scopes.orEmpty())) return@submitIOTask noScopes()
      val image = details.avatarUrl?.let { url -> CachingGEUserAvatarLoader.getInstance().requestAvatar(executor, url).join() }
      DetailsLoadingResult<GiteeUserDetailed>(details, image, null, false)
    }.successOnEdt(ModalityState.any()) {
      accountsModel.accountsListModel.contentsChanged(account)
      it
    }
  }

  private fun noToken() = DetailsLoadingResult<GiteeUserDetailed>(null, null, GiteeBundle.message("account.token.missing"), true)
  private fun noScopes() = DetailsLoadingResult<GiteeUserDetailed>(null, null, GiteeBundle.message("account.scopes.insufficient"), true)
}