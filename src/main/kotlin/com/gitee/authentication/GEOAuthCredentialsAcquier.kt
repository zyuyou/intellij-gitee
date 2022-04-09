// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.gitee.authentication.GEOAuthService.Companion.jacksonMapper
import com.gitee.authentication.accounts.GEAccountsUtils
import com.intellij.collaboration.auth.services.OAuthCredentialsAcquirer
import com.intellij.collaboration.auth.services.OAuthCredentialsAcquirerHttp
import com.intellij.util.Url

internal class GEOAuthCredentialsAcquirer(
  private val giteeAppCred: GEAccountsUtils.GEAppCredentials,
  private val authorizationCodeUrl: Url
) : OAuthCredentialsAcquirer<GECredentials> {

  override fun acquireCredentials(code: String): OAuthCredentialsAcquirer.AcquireCredentialsResult<GECredentials> {
    val tokenUrl = ACCESS_TOKEN_URL.addParameters(mapOf(
      "grant_type" to authGrantType,
      "client_id" to giteeAppCred.clientId,
      "client_secret" to giteeAppCred.clientSecret,
      "redirect_uri" to authorizationCodeUrl.toExternalForm(),
      "code" to code,
    ))

    return OAuthCredentialsAcquirerHttp.requestToken(tokenUrl) { body, _ ->
      val responseData = with(jacksonMapper) {
        propertyNamingStrategy = PropertyNamingStrategies.SnakeCaseStrategy()
        readValue(body, AuthorizationResponseData::class.java)
      }

      GECredentials(
        responseData.accessToken,
        responseData.refreshToken,
        responseData.expiresIn,
        responseData.tokenType,
        responseData.scope,
        responseData.createdAt
      )
    }
  }

  private data class AuthorizationResponseData(val accessToken: String,
                                               val refreshToken: String,
                                               val expiresIn: Long,
                                               val tokenType: String,
                                               val scope: String,
                                               val createdAt: Long)

  companion object {
    private const val authGrantType = "authorization_code"

    private val ACCESS_TOKEN_URL: Url
      get() = GEOAuthService.SERVICE_URL.resolve("token")
  }
}