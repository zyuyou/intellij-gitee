// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.gitee.authentication.ui

import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.collaboration.auth.ui.AccountsDetailsLoader
import com.intellij.collaboration.auth.ui.AccountsDetailsLoader.Result
import com.intellij.collaboration.ui.ExceptionUtil
import com.intellij.collaboration.util.ProgressIndicatorsProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.future.asDeferred
import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.data.GiteeAuthenticatedUser
import com.gitee.api.data.GiteeUserDetailed
import com.gitee.authentication.accounts.GEAccountManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.util.GESecurityUtil
import com.gitee.i18n.GiteeBundle
import com.gitee.util.CachingGEUserAvatarLoader
import java.awt.Image

internal class GEAccountsDetailsLoader(private val accountManager: GEAccountManager,
                                       private val indicatorsProvider: ProgressIndicatorsProvider,
                                       private val accountsModel: GEAccountsListModel)
  : AccountsDetailsLoader<GiteeAccount, GiteeUserDetailed> {

  override fun loadDetailsAsync(account: GiteeAccount): Deferred<Result<GiteeUserDetailed>> {
    val executor = createExecutor(account) ?: return CompletableDeferred<Result<GiteeUserDetailed>>(
      Result.Error(GiteeBundle.message("account.token.missing"), true))

    return ProgressManager.getInstance().submitIOTask(indicatorsProvider, true) {
      doLoadDetails(executor, it, account)
    }.asDeferred()
  }

  private fun doLoadDetails(executor: GiteeApiRequestExecutor, indicator: ProgressIndicator, account: GiteeAccount)
    : Result<GiteeAuthenticatedUser> {

    val (details, _) = try {
      GESecurityUtil.loadCurrentUserWithScopes(executor, indicator, account.server)
    }
    catch (e: Throwable) {
      val errorMessage = ExceptionUtil.getPresentableMessage(e)
      return Result.Error(errorMessage, false)
    }
//    if (!GESecurityUtil.isEnoughScopes(scopes.orEmpty())) {
//      return Result.Error(GiteeBundle.message("account.scopes.insufficient"), true)
//    }

    return Result.Success(details)
  }

  override fun loadAvatarAsync(account: GiteeAccount, url: String): Deferred<Image?> {
    val apiExecutor = createExecutor(account) ?: return CompletableDeferred<Image?>(null).apply { complete(null) }
    return CachingGEUserAvatarLoader.getInstance().requestAvatar(apiExecutor, url).asDeferred()
  }

  private fun createExecutor(account: GiteeAccount): GiteeApiRequestExecutor? {
    val token = accountsModel.newCredentials.getOrElse(account) {
      accountManager.findCredentials(account)
    } ?: return null
    return service<GiteeApiRequestExecutor.Factory>().create(token)
  }
}