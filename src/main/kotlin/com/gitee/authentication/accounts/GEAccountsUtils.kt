package com.gitee.authentication.accounts

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.gitee.authentication.GECredentials
import com.gitee.authentication.GEOAuthService
import com.gitee.authentication.getGEOAuthRequest
import com.gitee.authentication.ui.GEAccountsListModel
import com.gitee.authorization.getGERefreshRequest
import com.gitee.i18n.GiteeBundle.message
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.ProgressIndicatorUtils
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.util.alsoIfNull
import com.intellij.util.concurrency.annotations.RequiresEdt
import java.util.concurrent.CompletableFuture

internal object GEAccountsUtils {
  data class GEAppCredentials(val clientId: String, val clientSecret: String)

  private val LOG = logger<GEOAuthService>()

  val jacksonMapper: ObjectMapper get() = jacksonObjectMapper()

  private const val APP_CLIENT_ID: String = "b7837ec65bcb294b0e2a31e5669b788a3185829524af4d818c3c2a35b186366d"
  private const val APP_CLIENT_SECRET: String = "85891acdb745502e19e02e3bbcd405dd303190c8a3fcf29a6ca7a2796b76f918"

  /**
   * 获取默认Gitee应用凭证
   * */
  fun getDefaultGEAppCredentials(): GEAppCredentials {
    return GEAppCredentials(APP_CLIENT_ID, APP_CLIENT_SECRET)
  }

  /**
   * 尝试重新登录
   * */
  fun tryToReLogin(project: Project): GECredentials? {
    var credentialsFuture: CompletableFuture<GECredentials> = CompletableFuture()

    return try {
      ProgressManager.getInstance().runProcessWithProgressSynchronously(ThrowableComputable {
        val request = getGEOAuthRequest()
        credentialsFuture = GEOAuthService.instance.authorize(request)
        ProgressIndicatorUtils.awaitWithCheckCanceled(credentialsFuture)
      }, message("login.to.gitee"), true, project)
    }
    catch (t: Throwable) {
      credentialsFuture.cancel(true)
      null
    }
  }

  /**
   * Returns the user's credentials if the access token is still valid, otherwise updates the credentials and returns updated.
   */
  @RequiresEdt
  fun getOrUpdateUserCredentials(oAuthService: GEOAuthService,
                                 accountManager: GEAccountManager,
                                 account: GiteeAccount): GECredentials? =
    accountManager.findCredentials(account)?.let { credentials ->
      if (credentials.isAccessTokenValid()) return credentials

      val refreshRequest = getGERefreshRequest(credentials.refreshToken)
      val credentialFuture = oAuthService.updateAccessToken(refreshRequest)

      return try {
        ProgressManager.getInstance().runProcessWithProgressSynchronously(ThrowableComputable {
          val newCred = ProgressIndicatorUtils.awaitWithCheckCanceled(credentialFuture)
          accountManager.updateAccount(account, newCred)

          newCred
        }, message("account.update.credentials.progress.title"), true, null)
      }
      catch (e: RuntimeException) {
        val message = e.cause?.cause?.localizedMessage.alsoIfNull { e.localizedMessage }
        LOG.warn("Failed to update user credentials:\n$message")

        null
      }
    }

  @RequiresEdt
  private fun updateAccountsList(accountsListModel: GEAccountsListModel, accountManager: GEAccountManager) {
    val newTokensMap = mutableMapOf<GiteeAccount, GECredentials?>()

    newTokensMap.putAll(accountsListModel.newCredentials)

    for (account in accountsListModel.accounts) {
      newTokensMap.putIfAbsent(account, null)
    }

    accountManager.updateAccounts(newTokensMap)
    accountsListModel.clearNewCredentials()
  }
}