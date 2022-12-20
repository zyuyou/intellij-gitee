// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.collaboration.auth.services.*
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.Url
import com.intellij.util.Urls.newFromEncoded
import org.jetbrains.ide.BuiltInServerManager
import org.jetbrains.ide.RestService
import java.util.*
import java.util.concurrent.CompletableFuture

@Service
internal class GEOAuthService : OAuthServiceBase<GECredentials>(), OAuthServiceWithRefresh<GECredentials> {
  override val name: String get() = SERVICE_NAME

  fun authorize(): CompletableFuture<GECredentials> {
    return authorize(GEOAuthRequest(GEAccountsUtil.getDefaultGEAppCredentials()))
  }

  override fun revokeToken(token: String) {
    TODO("Not yet implemented")
  }

  private class GEOAuthRequest(giteeAppCred: GEAccountsUtil.GEAppCredentials) : OAuthRequest<GECredentials> {
    private val port: Int get() = BuiltInServerManager.getInstance().port

    private val codeVerifier = PkceUtils.generateCodeVerifier()

    private val codeChallenge = PkceUtils.generateShaCodeChallenge(codeVerifier, Base64.getEncoder())

    override val authorizationCodeUrl: Url
      get() = newFromEncoded("http://127.0.0.1:$port/${RestService.PREFIX}/$SERVICE_NAME/authorization_code")

    override val credentialsAcquirer: OAuthCredentialsAcquirer<GECredentials> =
      GEOAuthCredentialsAcquirer(giteeAppCred, authorizationCodeUrl)

    override val authUrlWithParameters: Url = AUTHORIZE_URL.addParameters(
      mapOf(
        "client_id" to giteeAppCred.clientId,
        "scope" to GEAccountsUtil.APP_CLIENT_SCOPE,
        "redirect_uri" to authorizationCodeUrl.toExternalForm(),
        "response_type" to "code",
      )
    )

    companion object {
      private val AUTHORIZE_URL: Url
        get() = SERVICE_URL.resolve("authorize")
    }
  }

  // TODO: fix case when some updateAccessToken are started or auth flow is started before
  override fun updateAccessToken(refreshTokenRequest: OAuthServiceWithRefresh.RefreshTokenRequest): CompletableFuture<GECredentials> =

    ProgressManager.getInstance().submitIOTask(EmptyProgressIndicator()) {
      val response = OAuthCredentialsAcquirerHttp.requestToken(refreshTokenRequest.refreshTokenUrlWithParameters)

      if (response.statusCode() == 200) {
        val responseData = with(GEAccountsUtil.jacksonMapper) {
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
