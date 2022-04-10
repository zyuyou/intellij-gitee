// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authorization

import com.gitee.authentication.GEOAuthService
import com.intellij.collaboration.auth.services.OAuthServiceWithRefresh
import com.intellij.util.Url

internal fun getGERefreshRequest(refreshToken: String): GERefreshTokenRequest {
  return GERefreshTokenRequest(refreshToken)
}

internal class GERefreshTokenRequest(
  override val refreshToken: String
) : OAuthServiceWithRefresh.RefreshTokenRequest {

  override val refreshTokenUrlWithParameters: Url = AUTHORIZE_URL.addParameters(mapOf(
    "grant_type" to refreshGrantType,
    "refresh_token" to refreshToken,
  ))

  companion object {
    private const val refreshGrantType = "refresh_token"

    private val AUTHORIZE_URL: Url
      get() = GEOAuthService.SERVICE_URL.resolve("token")
  }
}
