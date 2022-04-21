// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication

import com.gitee.api.GiteeServerPath
import com.intellij.collaboration.auth.OAuthCallbackHandlerBase
import com.intellij.collaboration.auth.services.OAuthService
import com.intellij.util.Urls
import com.intellij.util.io.isLocalOrigin
import com.intellij.util.io.referrer
import io.netty.handler.codec.http.HttpRequest

internal class GEOAuthCallbackHandler : OAuthCallbackHandlerBase() {
  override fun oauthService(): OAuthService<*> = GEOAuthService.instance

  override fun handleAcceptCode(isAccepted: Boolean): AcceptCodeHandleResult {
    val redirectUrl = if (isAccepted) {
      GEOAuthService.SERVICE_URL.resolve("intellij/complete")
    } else {
      GEOAuthService.SERVICE_URL.resolve("intellij/error")
    }
    return AcceptCodeHandleResult.Redirect(redirectUrl)
  }

  override fun isOriginAllowed(request: HttpRequest): OriginCheckResult {
    if (request.isLocalOrigin()) return OriginCheckResult.ALLOW

    val uri = request.referrer ?: return OriginCheckResult.ALLOW

    try {
      val parsedUri = Urls.parse(uri, false) ?: return OriginCheckResult.FORBID

      return if (parsedUri.authority == GiteeServerPath.DEFAULT_HOST)
        OriginCheckResult.ALLOW else OriginCheckResult.FORBID

    } catch (ignored: Exception) {
    }

    return OriginCheckResult.FORBID
  }
}