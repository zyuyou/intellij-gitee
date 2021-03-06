/*
 *  Copyright 2016-2019 码云 - Gitee
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.gitee.api

import com.gitee.api.GiteeApiRequestExecutor.Factory.Companion.getInstance
import com.gitee.api.GiteeServerPath.Companion.from
import com.gitee.api.data.GiteeAuthorization
import com.gitee.api.data.GiteeErrorMessage
import com.gitee.authentication.util.GiteeTokenCreator
import com.gitee.exceptions.*
import com.gitee.util.GiteeSettings
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.DumbProgressIndicator
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.EventDispatcher
import com.intellij.util.ThrowableConvertor
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.HttpSecurityUtil
import com.intellij.util.io.RequestBuilder
import org.jetbrains.annotations.CalledInAny
import org.jetbrains.annotations.CalledInBackground
import org.jetbrains.annotations.TestOnly
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.net.HttpURLConnection
import java.util.*
import java.util.zip.GZIPInputStream


/**
 * Executes API requests taking care of authentication, headers, proxies, timeouts, etc.
 *
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/api/GithubApiRequestExecutor.kt
 * @author JetBrains s.r.o.
 */
sealed class GiteeApiRequestExecutor {
  protected val authDataChangedEventDispatcher = EventDispatcher.create(AuthDataChangeListener::class.java)

  @CalledInBackground
  @Throws(IOException::class, ProcessCanceledException::class)
  abstract fun <T> execute(indicator: ProgressIndicator, request: GiteeApiRequest<T>): T

  @TestOnly
  @CalledInBackground
  @Throws(IOException::class, ProcessCanceledException::class)
  fun <T> execute(request: GiteeApiRequest<T>): T = execute(EmptyProgressIndicator(), request)

  fun addListener(listener: AuthDataChangeListener, disposable: Disposable) =
    authDataChangedEventDispatcher.addListener(listener, disposable)

  fun addListener(disposable: Disposable, listener: () -> Unit) =
    authDataChangedEventDispatcher.addListener(object : AuthDataChangeListener {
      override fun authDataChanged() {
        listener()
      }
    }, disposable)

  class WithPasswordOAuth2 internal constructor(giteeSettings: GiteeSettings) : Base(giteeSettings) {

    @Throws(IOException::class, ProcessCanceledException::class)
    override fun <T> execute(indicator: ProgressIndicator, request: GiteeApiRequest<T>): T {
      indicator.checkCanceled()

      return createRequestBuilder(request).execute(request, indicator)
    }
  }

  class WithTokenAuth internal constructor(giteeSettings: GiteeSettings,
                                           token: String,
                                           private val useProxy: Boolean) : Base(giteeSettings) {

    @Volatile
    internal var token: String = token
      set(value) {
        field = value
        authDataChangedEventDispatcher.multicaster.authDataChanged()
      }

    @Throws(IOException::class, ProcessCanceledException::class)
    override fun <T> execute(indicator: ProgressIndicator, request: GiteeApiRequest<T>): T {
      indicator.checkCanceled()
      return createRequestBuilder(request)
        .tuner { connection ->
          request.additionalHeaders.forEach(connection::addRequestProperty)
          connection.addRequestProperty(HttpSecurityUtil.AUTHORIZATION_HEADER_NAME, "Token $token")
        }
        .useProxy(useProxy)
        .execute(request, indicator)
    }
  }

  class WithTokensAuth internal constructor(giteeSettings: GiteeSettings, tokens: Pair<String, String>,
                                            private val authDataChangedSupplier: (tokens: Pair<String, String>) -> Unit) : Base(giteeSettings) {

    @Volatile
    internal var tokens: Pair<String, String> = tokens
      set(value) {
        field = value
        authDataChangedEventDispatcher.multicaster.authDataChanged()
      }

    @Throws(IOException::class, ProcessCanceledException::class)
    override fun <T> execute(indicator: ProgressIndicator, request: GiteeApiRequest<T>): T {
      indicator.checkCanceled()
      return executeWithAccessToken(indicator, request)
    }

    private fun <T> executeWithAccessToken(indicator: ProgressIndicator, request: GiteeApiRequest<T>): T {
      indicator.checkCanceled()

      return try {
        createRequestBuilder(request)
          .tuner { connection ->
            request.additionalHeaders.forEach(connection::addRequestProperty)
            connection.addRequestProperty("Authorization", "token ${tokens.first}")
          }
          .execute(request, indicator)

      } catch (e: GiteeAccessTokenExpiredException) {
        if (tokens.second == "") throw e

        val serverPath = from(request.url.substringBefore('?'))

        val authorization: GiteeAuthorization = GiteeTokenCreator(
          from(serverPath.toUrl().removeSuffix(serverPath.suffix ?: "")),
          getInstance().create(),
          DumbProgressIndicator()
        ).updateMaster(tokens.second)

        tokens = authorization.accessToken to authorization.refreshToken

        authDataChangedSupplier(tokens)

        return createRequestBuilder(request)
          .tuner { connection ->
            request.additionalHeaders.forEach(connection::addRequestProperty)
            connection.addRequestProperty("Authorization", "token ${authorization.accessToken}")
          }
          .execute(request, indicator)
      }
    }
  }

  abstract class Base(private val giteeSettings: GiteeSettings) : GiteeApiRequestExecutor() {

    protected fun <T> RequestBuilder.execute(request: GiteeApiRequest<T>, indicator: ProgressIndicator): T {
      indicator.checkCanceled()
      try {
        LOG.debug("Request: ${request.url} ${request.operationName} : Connecting")
        return connect {
          val connection = it.connection as HttpURLConnection

          if (request is GiteeApiRequest.WithBody) {
            LOG.debug("Request: ${connection.requestMethod} ${connection.url} ${connection.requestMethod} with body:\n${request.body} : Connected")
            request.body?.let { body -> it.write(body) }
          } else {
            LOG.debug("Request: ${connection.requestMethod} ${connection.url} ${connection.requestMethod} : Connected")
          }
          checkResponseCode(connection)
          indicator.checkCanceled()

          val result = request.extractResult(createResponse(it, indicator))
          LOG.debug("Request: ${connection.requestMethod} ${connection.url} ${connection.requestMethod} : Result extracted")

          result
        }
      } catch (e: GiteeStatusCodeException) {
        @Suppress("UNCHECKED_CAST")
        if (request is GiteeApiRequest.Get.Optional<*> && e.statusCode == HttpURLConnection.HTTP_NOT_FOUND) return null as T else throw e
      } catch (e: GiteeConfusingException) {
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
        is GiteeApiRequest.Put -> HttpRequests.put(request.url, request.bodyMimeType)
        is GiteeApiRequest.Patch -> HttpRequests.patch(request.url, request.bodyMimeType)
        is GiteeApiRequest.Head -> HttpRequests.head(request.url)
        is GiteeApiRequest.Delete -> {
          if (request.body == null) HttpRequests.delete(request.url) else HttpRequests.delete(request.url, request.bodyMimeType)
        }
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

      LOG.debug("Request: ${connection.requestMethod} ${connection.url}: Error $statusLine body:\n $errorText")

      val jsonError = getJsonError(connection, errorText)
      jsonError ?: LOG.debug("Request: ${connection.requestMethod} ${connection.url} : Unable to parse JSON error")

      throw when (connection.responseCode) {
        HttpURLConnection.HTTP_NOT_FOUND,
        HttpURLConnection.HTTP_UNAUTHORIZED,
        HttpURLConnection.HTTP_PAYMENT_REQUIRED,
        HttpURLConnection.HTTP_FORBIDDEN -> {

          when {
            jsonError?.containsReasonMessage("API rate limit exceeded") == true -> GiteeRateLimitExceededException(jsonError.presentableError)
            jsonError?.containsReasonMessage("Access token is expired") == true -> GiteeAccessTokenExpiredException(jsonError.presentableError)
            jsonError?.containsErrorMessage("invalid_grant") == true -> GiteeAuthenticationException("${jsonError.error} : ${jsonError.errorDescription
              ?: ""}")
            else -> GiteeAuthenticationException("Request response: " + (jsonError?.presentableError
              ?: if (errorText != "") errorText else statusLine))
          }
        }
        else -> {
          if (jsonError != null) {
            GiteeStatusCodeException("$statusLine - ${jsonError.presentableError}", jsonError, connection.responseCode)
          } else {
            GiteeStatusCodeException("$statusLine - $errorText", connection.responseCode)
          }
        }
      }
    }

    private fun getErrorText(connection: HttpURLConnection): String {
//      return connection.errorStream?.let { it -> InputStreamReader(it).use { it.readText() } } ?: ""
      val errorStream = connection.errorStream ?: return ""
      val stream = if (connection.contentEncoding == "gzip") GZIPInputStream(errorStream) else errorStream
      return InputStreamReader(stream, Charsets.UTF_8).use { it.readText() }
    }

    private fun getJsonError(connection: HttpURLConnection, errorText: String): GiteeErrorMessage? {
      if (!connection.contentType.startsWith(GiteeApiContentHelper.JSON_MIME_TYPE)) return null
      return try {
        return GiteeApiContentHelper.fromJson(errorText)
      } catch (jse: GiteeJsonException) {
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

  class Factory internal constructor(private val giteeSettings: GiteeSettings) {

    @CalledInAny
    fun create(token: String): WithTokenAuth {
      return create(token, true)
    }

    @CalledInAny
    fun create(token: String, useProxy: Boolean = true): WithTokenAuth {
      return WithTokenAuth(giteeSettings, token, useProxy)
    }

    @CalledInAny
    fun create(tokens: Pair<String, String>, authDataChangedSupplier: (tokens: Pair<String, String>) -> Unit): WithTokensAuth {
      return WithTokensAuth(giteeSettings, tokens, authDataChangedSupplier)
    }

    @CalledInAny
    fun create(): WithPasswordOAuth2 {
      return WithPasswordOAuth2(giteeSettings)
    }

    companion object {
      @JvmStatic
      fun getInstance(): Factory = service()
    }
  }

  companion object {
    private val LOG = logger<GiteeApiRequestExecutor>()
  }

  interface AuthDataChangeListener : EventListener {
    fun authDataChanged()
  }

  enum class TokenHeaderType {
    TOKEN, BEARER
  }
}