// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.collaboration.auth.services.OAuthServiceBase
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.util.Url
import com.intellij.util.Urls.newFromEncoded
import java.util.concurrent.CompletableFuture

@Service
internal class GEOAuthService : OAuthServiceBase<GECredentials>() {
  override val name: String get() = SERVICE_NAME

  fun authorize(): CompletableFuture<GECredentials> {
    val request = getGEOAuthRequest()
    return authorize(request)
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
}
