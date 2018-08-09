// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api

import com.gitee.authentication.accounts.GiteeAccountManager
import com.google.gson.JsonParseException
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.ThrowableConvertor
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.RequestBuilder
import org.jetbrains.annotations.CalledInAny
import org.jetbrains.annotations.TestOnly
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.net.HttpURLConnection

/**
 * Executes API requests taking care of authentication, headers, proxies, timeouts, etc.
 */
sealed class GiteeApiRequestExecutor {

  @Throws(IOException::class, ProcessCanceledException::class)
  abstract fun <T> execute(indicator: ProgressIndicator, request: GiteeApiRequest<T>): T

  @TestOnly
  @Throws(IOException::class, ProcessCanceledException::class)
  fun <T> execute(request: GiteeApiRequest<T>): T = execute(EmptyProgressIndicator(), request)

  class WithTokenAuth internal constructor(giteeSettings: com.gitee.util.GiteeSettings,
                                           private val token: String,
                                           private val useProxy: Boolean) : Base(giteeSettings) {

    @Throws(IOException::class, ProcessCanceledException::class)
    override fun <T> execute(indicator: ProgressIndicator, request: GiteeApiRequest<T>): T {
      indicator.checkCanceled()
      return createRequestBuilder(request)
        .tuner { connection -> connection.addRequestProperty("Authorization", "token $token") }
        .useProxy(useProxy)
        .execute(request, indicator)
    }
  }

//  class WithRrefreshTokenOAuth2 internal constructor(giteeSettings: GiteeSettings) : Base(giteeSettings) {
//
//    @Throws(IOException::class, ProcessCanceledException::class)
//    override fun <T> execute(indicator: ProgressIndicator, request: GiteeApiRequest<T>): T {
//      indicator.checkCanceled()
//
//      return createRequestBuilder(request).execute(request, indicator)
//    }
//  }

  class WithPasswordOAuth2 internal constructor(giteeSettings: com.gitee.util.GiteeSettings) : Base(giteeSettings) {

    @Throws(IOException::class, ProcessCanceledException::class)
    override fun <T> execute(indicator: ProgressIndicator, request: GiteeApiRequest<T>): T {
      indicator.checkCanceled()

      return createRequestBuilder(request).execute(request, indicator)
    }
  }

  class WithTokensAuth internal constructor(giteeSettings: com.gitee.util.GiteeSettings,
                                            private val accountManager: GiteeAccountManager,
                                            private var tokens: Pair<String, String>,
                                            private val refreshTokenSupplier: (refreshToken: String) -> Triple<com.gitee.authentication.accounts.GiteeAccount, String, String>) : Base(giteeSettings) {

    @Throws(IOException::class, ProcessCanceledException::class)
    override fun <T> execute(indicator: ProgressIndicator, request: GiteeApiRequest<T>): T {
      indicator.checkCanceled()
      return executeWithAccessToken(indicator, request)
    }

    private fun <T> executeWithAccessToken(indicator: ProgressIndicator, request: GiteeApiRequest<T>): T {
      indicator.checkCanceled()

      return try {
        createRequestBuilder(request)
          .tuner { connection -> connection.addRequestProperty("Authorization", "token ${tokens.first}") }
          .execute(request, indicator)

      } catch (e: com.gitee.exceptions.GiteeAccessTokenExpiredException) {
        if (tokens.second == "") throw e

        val (account, newAccessToken, newRefreshToken) = refreshTokenSupplier(tokens.second)
        tokens = newAccessToken to newRefreshToken
        accountManager.updateAccountToken(account, "$newAccessToken&$newRefreshToken")

        return createRequestBuilder(request)
          .tuner { connection -> connection.addRequestProperty("Authorization", "token $newAccessToken") }
          .execute(request, indicator)
      }
    }
  }

//  class WithBasicAuth internal constructor(giteeSettings: GiteeSettings,
//                                           private val login: String,
//                                           private val password: CharArray,
//                                           private val twoFactorCodeSupplier: Supplier<String?>) : Base(giteeSettings) {
//
//    private var twoFactorCode: String? = null
//
//    @Throws(IOException::class, ProcessCanceledException::class)
//    override fun <T> execute(indicator: ProgressIndicator, request: GiteeApiRequest<T>): T {
//      indicator.checkCanceled()
//      val basicHeaderValue = HttpSecurityUtil.createBasicAuthHeaderValue(login, password)
//      return executeWithBasicHeader(indicator, request, basicHeaderValue)
//    }
//
//    private fun <T> executeWithBasicHeader(indicator: ProgressIndicator, request: GiteeApiRequest<T>, header: String): T {
//      indicator.checkCanceled()
//      return try {
//        createRequestBuilder(request)
//          .tuner { connection ->
//            connection.addRequestProperty(HttpSecurityUtil.AUTHORIZATION_HEADER_NAME, "Basic $header")
//            twoFactorCode?.let { connection.addRequestProperty(OTP_HEADER_NAME, it) }
//          }
//          .execute(request, indicator)
//      } catch (e: GiteeTwoFactorAuthenticationException) {
//        twoFactorCode = twoFactorCodeSupplier.get() ?: throw e
//        executeWithBasicHeader(indicator, request, header)
//      }
//    }
//  }

  abstract class Base(private val giteeSettings: com.gitee.util.GiteeSettings) : GiteeApiRequestExecutor() {

    protected fun <T> RequestBuilder.execute(request: GiteeApiRequest<T>, indicator: ProgressIndicator): T {
      indicator.checkCanceled()
      try {
        return connect {
          val connection = it.connection as HttpURLConnection

          if (request is GiteeApiRequest.WithBody) {
            LOG.debug("Request: ${connection.requestMethod} ${connection.url} with body:\n${request.body}")
            it.write(request.body)
          } else {
            LOG.debug("Request: ${connection.requestMethod} ${connection.url}")
          }
          checkResponseCode(connection)
          indicator.checkCanceled()

          val result = request.extractResult(createResponse(it, indicator))
          LOG.debug("Request: ${connection.requestMethod} ${connection.url}: Success")

          result
        }
      } catch (e: com.gitee.exceptions.GiteeStatusCodeException) {
        @Suppress("UNCHECKED_CAST")
        if (request is GiteeApiRequest.Get.Optional<*> && e.statusCode == HttpURLConnection.HTTP_NOT_FOUND) return null as T else throw e
      } catch (e: com.gitee.exceptions.GiteeConfusingException) {
        if (request.operationName != null) {
          val errorText = "Can't ${request.operationName}"
          e.setDetails(errorText)
          LOG.debug(errorText, e)
        }
        throw e
      }
    }

    protected fun createRequestBuilder(request: GiteeApiRequest<*>): RequestBuilder {
      return when (request) {
        is GiteeApiRequest.Get -> HttpRequests.request(request.url)
        is GiteeApiRequest.Post -> HttpRequests.post(request.url, request.bodyMimeType)
        is GiteeApiRequest.Patch -> HttpRequests.patch(request.url, request.bodyMimeType)
        is GiteeApiRequest.Head -> HttpRequests.head(request.url)
        is GiteeApiRequest.Delete -> HttpRequests.delete(request.url)
        else -> throw UnsupportedOperationException("${request.javaClass} is not supported")
      }
        .connectTimeout(giteeSettings.connectionTimeout)
        .userAgent("Intellij IDEA Gitee Plugin")
        .throwStatusCodeException(false)
        .forceHttps(true)
        .accept(request.acceptMimeType)
    }

    @Throws(IOException::class)
    private fun checkResponseCode(connection: HttpURLConnection) {
      if (connection.responseCode < 400) return

      val statusLine = "${connection.responseCode} ${connection.responseMessage}"
      val errorText = getErrorText(connection)
      val jsonError = getJsonError(connection, errorText)

      LOG.debug("Request: ${connection.requestMethod} ${connection.url}: Error $statusLine body:\n $errorText")

      throw when (connection.responseCode) {
        HttpURLConnection.HTTP_UNAUTHORIZED,
        HttpURLConnection.HTTP_PAYMENT_REQUIRED,
        HttpURLConnection.HTTP_FORBIDDEN -> {

          when {
            jsonError?.containsReasonMessage("API rate limit exceeded") == true -> com.gitee.exceptions.GiteeRateLimitExceededException(jsonError.message)
            jsonError?.containsReasonMessage("Access token is expired") == true -> com.gitee.exceptions.GiteeAccessTokenExpiredException(jsonError.message)
            jsonError?.containsErrorMessage("invalid_grant") == true -> com.gitee.exceptions.GiteeAuthenticationException("${jsonError.error} : ${jsonError.errorDescription
              ?: ""}")
            else -> com.gitee.exceptions.GiteeAuthenticationException("Request response: " + (jsonError?.message
              ?: if (errorText != "") errorText else statusLine))
          }
        }
        else -> {
          if (jsonError != null) {
            com.gitee.exceptions.GiteeStatusCodeException("$statusLine - ${jsonError.message}", jsonError, connection.responseCode)
          } else {
            com.gitee.exceptions.GiteeStatusCodeException("$statusLine - $errorText", connection.responseCode)
          }
        }
      }
    }

    private fun getErrorText(connection: HttpURLConnection): String {
      return connection.errorStream?.let { InputStreamReader(it).use { it.readText() } } ?: ""
    }

    private fun getJsonError(connection: HttpURLConnection, errorText: String): com.gitee.api.data.GiteeErrorMessage? {
      if (!connection.contentType.startsWith(GiteeApiContentHelper.JSON_MIME_TYPE)) return null
      return try {
        return GiteeApiContentHelper.fromJson(errorText)
      } catch (jse: JsonParseException) {
        LOG.debug(jse)
        null
      }
    }

    private fun createResponse(request: HttpRequests.Request, indicator: ProgressIndicator): GiteeApiResponse {
      return object : GiteeApiResponse {
        override fun findHeader(headerName: String): String? = request.connection.getHeaderField(headerName)

        override fun <T> readBody(converter: ThrowableConvertor<Reader, T, IOException>): T = request.getReader(indicator).use {
          converter.convert(it)
        }

        override fun <T> handleBody(converter: ThrowableConvertor<InputStream, T, IOException>): T = request.inputStream.use {
          converter.convert(it)
        }
      }
    }
  }

  class Factory internal constructor(private val giteeSettings: com.gitee.util.GiteeSettings,
                                     private val accountManager: GiteeAccountManager) {

    @CalledInAny
    fun create(token: String): WithTokenAuth {
      return create(token, true)
    }

    @CalledInAny
    fun create(token: String, useProxy: Boolean = true): WithTokenAuth {
      return GiteeApiRequestExecutor.WithTokenAuth(giteeSettings, token, useProxy)
    }

    @CalledInAny
    fun create(tokens: Pair<String, String>, refreshTokenSupplier: (refreshToken: String) -> Triple<com.gitee.authentication.accounts.GiteeAccount, String, String>): WithTokensAuth {
      return GiteeApiRequestExecutor.WithTokensAuth(giteeSettings, accountManager, tokens, refreshTokenSupplier)
    }

    @CalledInAny
    fun create(): WithPasswordOAuth2 {
      return GiteeApiRequestExecutor.WithPasswordOAuth2(giteeSettings)
    }

    companion object {
      @JvmStatic
      fun getInstance(): Factory = service()
    }
  }

  companion object {
    private val LOG = logger<GiteeApiRequestExecutor>()
  }
}