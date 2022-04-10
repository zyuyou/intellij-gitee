// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.gitee.authentication.accounts.GEAccountsUtils
import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.collaboration.auth.services.OAuthCredentialsAcquirerHttp
import com.intellij.collaboration.auth.services.OAuthServiceBase
import com.intellij.collaboration.auth.services.OAuthServiceWithRefresh
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.Url
import com.intellij.util.Urls.newFromEncoded
import java.util.concurrent.CompletableFuture

@Service
internal class GEOAuthService : OAuthServiceBase<GECredentials>(), OAuthServiceWithRefresh<GECredentials> {
  override val name: String get() = SERVICE_NAME

  fun authorize(): CompletableFuture<GECredentials> {
    val request = getGEOAuthRequest()
    return authorize(request)
  }

  // TODO: fix case when some updateAccessToken are started or auth flow is started before
  override fun updateAccessToken(refreshTokenRequest: OAuthServiceWithRefresh.RefreshTokenRequest): CompletableFuture<GECredentials> =

    ProgressManager.getInstance().submitIOTask(EmptyProgressIndicator()) {
      val response = OAuthCredentialsAcquirerHttp.requestToken(refreshTokenRequest.refreshTokenUrlWithParameters)

      if (response.statusCode() == 200) {
        val responseData = with(GEAccountsUtils.jacksonMapper) {
          propertyNamingStrategy = PropertyNamingStrategies.SnakeCaseStrategy()
          readValue(response.body(), RefreshResponseData::class.java)
        }

        GECredentials(
          responseData.accessToken,
          refreshTokenRequest.refreshToken,
          responseData.expiresIn,
          responseData.tokenType,
          responseData.scope,
          responseData.createdAt,
        )
      }
      else {
        throw RuntimeException(response.body().ifEmpty { "No token provided" })
      }
    }

  override fun revokeToken(token: String) {
    TODO("Not yet implemented")
  }

  companion object {
    private const val SERVICE_NAME = "gitee/oauth"

    val jacksonMapper: ObjectMapper get() = jacksonObjectMapper()

    val instance: GEOAuthService = service()

    val SERVICE_URL: Url = newFromEncoded("https://gitee.com/oauth")
  }

  private data class RefreshResponseData(val accessToken: String,
                                         val expiresIn: Long,
                                         val scope: String,
                                         val tokenType: String,
                                         val createdAt: Long)
}
