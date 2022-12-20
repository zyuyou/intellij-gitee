package com.gitee.authentication.ui

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.data.GiteeAuthenticatedUser
import com.gitee.api.data.GiteeUserDetailed
import com.gitee.authentication.accounts.GEAccountManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.util.GESecurityUtil
import com.gitee.i18n.GiteeBundle
import com.gitee.icons.GiteeIcons
import com.gitee.util.CachingGEUserAvatarLoader
import com.intellij.collaboration.auth.ui.LazyLoadingAccountsDetailsProvider
import com.intellij.collaboration.auth.ui.cancelOnRemoval
import com.intellij.collaboration.ui.ExceptionUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runUnderIndicator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import java.awt.Image

internal class GEAccountsDetailsProvider(
  scope: CoroutineScope,
  private val executorSupplier: suspend (GiteeAccount) -> GiteeApiRequestExecutor?
) : LazyLoadingAccountsDetailsProvider<GiteeAccount, GiteeUserDetailed>(scope, GiteeIcons.DefaultAvatar) {

  constructor(scope: CoroutineScope, accountManager: GEAccountManager, accountsModel: GEAccountsListModel)
    : this(scope, { getExecutor(accountManager, accountsModel, it) }) {
    cancelOnRemoval(accountsModel.accountsListModel)
  }

  constructor(scope: CoroutineScope, accountManager: GEAccountManager)
    : this(scope, { getExecutor(accountManager, it) }) {
    cancelOnRemoval(scope, accountManager)
  }

  override suspend fun loadAvatar(account: GiteeAccount, url: String): Image? {
    val apiExecutor = executorSupplier(account) ?: return null
    return CachingGEUserAvatarLoader.getInstance().requestAvatar(apiExecutor, url).await()
  }

  override suspend fun loadDetails(account: GiteeAccount): Result<GiteeUserDetailed> {
    val executor = try {
      executorSupplier(account)
    }
    catch (e: Exception) {
      null
    } ?: return Result.Error(GiteeBundle.message("account.credentials.missing"), true)
    return withContext(Dispatchers.IO) {
      runUnderIndicator {
        doLoadDetails(executor, account)
      }
    }
  }

  private fun doLoadDetails(executor: GiteeApiRequestExecutor, account: GiteeAccount) : Result<GiteeAuthenticatedUser> {
    val (details, _) = try {
      GESecurityUtil.loadCurrentUserWithScopes(executor, account.server)
    }
    catch (e: Throwable) {
      val errorMessage = ExceptionUtil.getPresentableMessage(e)
      return Result.Error(errorMessage, false)
    }

    return Result.Success(details)
  }

  companion object {
    private suspend fun getExecutor(accountManager: GEAccountManager, accountsModel: GEAccountsListModel, account: GiteeAccount)
      : GiteeApiRequestExecutor? {
      return accountsModel.newCredentials.getOrElse(account) {
        accountManager.findCredentials(account)
      }?.let { credentials ->
        service<GiteeApiRequestExecutor.Factory>().create(credentials) {
          newCredentials -> accountManager.updateAccount(account, newCredentials)
        }
      }
    }

    private suspend fun getExecutor(accountManager: GEAccountManager, account: GiteeAccount)
      : GiteeApiRequestExecutor? {
      return accountManager.findCredentials(account)?.let { credentials ->
        service<GiteeApiRequestExecutor.Factory>().create(credentials) {
            newCredentials -> accountManager.updateAccount(account, newCredentials)
        }
      }
    }
  }

}