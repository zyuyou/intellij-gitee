package com.gitee.authentication

import com.intellij.collaboration.auth.services.OAuthCredentialsAcquirer
import com.intellij.collaboration.auth.services.OAuthRequest
import com.intellij.util.Url
import com.intellij.util.Urls
import org.jetbrains.ide.BuiltInServerManager
import org.jetbrains.ide.RestService

//internal fun getGEOAuthRequest(): GEOAuthRequest {
//  val giteeAppCred = GEAccountsUtil.getDefaultGEAppCredentials()
//  return GEOAuthRequest(giteeAppCred)
//}

//internal class GEOAuthRequest(giteeAppCred: GEAccountsUtil.GEAppCredentials): OAuthRequest<GECredentials> {
//  private val port: Int get() = BuiltInServerManager.getInstance().port
//
//  override val authorizationCodeUrl: Url
//    get() = Urls.newFromEncoded("http://127.0.0.1:$port/${RestService.PREFIX}/${GEOAuthService.instance.name}/authorization_code")
//
//  override val credentialsAcquirer: OAuthCredentialsAcquirer<GECredentials> =
//    GEOAuthCredentialsAcquirer(giteeAppCred, authorizationCodeUrl)
//
//  override val authUrlWithParameters: Url = AUTHORIZE_URL.addParameters(mapOf(
//    "client_id" to giteeAppCred.clientId,
//    "scope" to GEAccountsUtil.APP_CLIENT_SCOPE,
//    "redirect_uri" to authorizationCodeUrl.toExternalForm(),
//    "response_type" to responseType,
//  ))
//
//  companion object {
//    private const val responseType = "code"
//
//    private val AUTHORIZE_URL: Url
//      get() = GEOAuthService.SERVICE_URL.resolve("authorize")
//  }
//
//}