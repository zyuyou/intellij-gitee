// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api

import com.gitee.api.data.GiteeResponsePage
import com.google.gson.reflect.TypeToken
import com.intellij.util.ThrowableConvertor
import java.io.IOException

/**
 * Represents an API request with strictly defined response type
 */
sealed class GiteeApiRequest<T>(val url: String) {

  var operationName: String? = null
  abstract val acceptMimeType: String?

  @Throws(IOException::class)
  abstract fun extractResult(response: GiteeApiResponse): T

  fun withOperationName(name: String): GiteeApiRequest<T> {
    operationName = name
    return this
  }

  abstract class Get<T> @JvmOverloads constructor(url: String,
                                                  override val acceptMimeType: String? = null) : GiteeApiRequest<T>(url) {

    abstract class Optional<T> @JvmOverloads constructor(url: String,
                                                         acceptMimeType: String? = null) : Get<T?>(url, acceptMimeType) {
      companion object {
        inline fun <reified T> json(url: String): Optional<T> = Json(url, T::class.java)
      }

      open class Json<T>(url: String, clazz: Class<T>) : Optional<T>(url, GiteeApiContentHelper.JSON_MIME_TYPE) {
        private val typeToken = TypeToken.get(clazz)

        override fun extractResult(response: GiteeApiResponse): T = parseJsonResponse(response, typeToken)
      }
    }

    companion object {
      inline fun <reified T> json(url: String): Get<T> = Json(url, T::class.java)

      inline fun <reified T> jsonList(url: String): Get<List<T>> = JsonList(url, T::class.java)

      inline fun <reified T> jsonPage(url: String): Get<GiteeResponsePage<T>> = JsonPage(url, T::class.java)

      inline fun <reified T> jsonPage2(url: String): Get<GiteeResponsePage<T>> = JsonPage2(url, T::class.java)

//      inline fun <reified T> jsonSearchPage(url: String): Get<GiteeResponsePage<T>> = JsonSearchPage(url, T::class.java)
    }

    open class Json<T>(url: String, clazz: Class<T>) : Get<T>(url, GiteeApiContentHelper.JSON_MIME_TYPE) {
      private val typeToken = TypeToken.get(clazz)

      override fun extractResult(response: GiteeApiResponse): T = parseJsonResponse(response, typeToken)
    }

    open class JsonList<T>(url: String, clazz: Class<T>) : Get<List<T>>(url, GiteeApiContentHelper.JSON_MIME_TYPE) {
      @Suppress("UNCHECKED_CAST")
      private val typeToken = TypeToken.getParameterized(List::class.java, clazz) as TypeToken<List<T>>

      override fun extractResult(response: GiteeApiResponse): List<T> = parseJsonResponse(response, typeToken)
    }

    open class JsonPage<T>(url: String, clazz: Class<T>) : Get<GiteeResponsePage<T>>(url, GiteeApiContentHelper.JSON_MIME_TYPE) {
      @Suppress("UNCHECKED_CAST")
      private val pageTypeToken = TypeToken.getParameterized(GiteeResponsePage::class.java, clazz) as TypeToken<GiteeResponsePage<T>>

      override fun extractResult(response: GiteeApiResponse): GiteeResponsePage<T> {
        return GiteeResponsePage.parseFromResult(parseJsonResponse(response, pageTypeToken), url)
      }
    }

    open class JsonPage2<T>(url: String, clazz: Class<T>) : Get<GiteeResponsePage<T>>(url, GiteeApiContentHelper.JSON_MIME_TYPE) {
      @Suppress("UNCHECKED_CAST")
      private val typeToken = TypeToken.getParameterized(List::class.java, clazz) as TypeToken<List<T>>

      override fun extractResult(response: GiteeApiResponse): GiteeResponsePage<T> {
        return GiteeResponsePage.parseFromResult(parseJsonResponse(response, typeToken), url)
      }
    }

//    open class JsonSearchPage<T>(url: String, clazz: Class<T>) : Get<GiteeResponsePage<T>>(url, GiteeApiContentHelper.JSON_MIME_TYPE) {
//      private val typeToken = TypeToken.getParameterized(GiteeSearchResult::class.java, clazz) as TypeToken<GiteeSearchResult<T>>
//
//      override fun extractResult(response: GiteeApiResponse): GiteeResponsePage<T> {
//        return GiteeResponsePage.parseFromHeader(parseJsonResponse(response, typeToken).items,
//                                                  response.findHeader(GiteeResponsePage.HEADER_NAME))
//      }
//    }
  }

  abstract class Head<T> @JvmOverloads constructor(url: String,
                                                   override val acceptMimeType: String? = null) : GiteeApiRequest<T>(url)

  abstract class WithBody<T>(url: String) : GiteeApiRequest<T>(url) {
    abstract val body: String
    abstract val bodyMimeType: String
  }

  abstract class Post<T> @JvmOverloads constructor(override val body: String,
                                                   override val bodyMimeType: String,
                                                   url: String,
                                                   override val acceptMimeType: String? = null) : GiteeApiRequest.WithBody<T>(url) {
    companion object {
      inline fun <reified T> json(url: String, body: Any): Post<T> = Json(url, body, T::class.java)
      inline fun <reified T> formUrlEncoded(url: String, body: Any): Post<T> = FormUrlEncoded(url, body, T::class.java)
    }

    open class Json<T>(url: String, body: Any, clazz: Class<T>) : Post<T>(
      GiteeApiContentHelper.toJson(body),
      GiteeApiContentHelper.JSON_MIME_TYPE,
      url,
      GiteeApiContentHelper.JSON_MIME_TYPE
    ) {

      private val typeToken = TypeToken.get(clazz)

      override fun extractResult(response: GiteeApiResponse): T = parseJsonResponse(response, typeToken)
    }

    open class FormUrlEncoded<T>(url: String, body: Any, clazz: Class<T>) : Post<T>(
      GiteeApiContentHelper.toFormUrlEncoded(body),
      GiteeApiContentHelper.FORM_URLENCODED_MINE_TYPE,
      url,
      GiteeApiContentHelper.JSON_MIME_TYPE
    ) {

      private val typeToken = TypeToken.get(clazz)

      override fun extractResult(response: GiteeApiResponse): T = parseJsonResponse(response, typeToken)
    }
  }

  abstract class Patch<T> @JvmOverloads constructor(override val body: String,
                                                    override val bodyMimeType: String,
                                                    url: String,
                                                    override val acceptMimeType: String? = null) : GiteeApiRequest.WithBody<T>(url) {
    companion object {
      inline fun <reified T> json(url: String, body: Any): Patch<T> = Json(url, body, T::class.java)
    }

    open class Json<T>(url: String, body: Any, clazz: Class<T>) : Patch<T>(GiteeApiContentHelper.toJson(body),
      GiteeApiContentHelper.JSON_MIME_TYPE,
      url,
      GiteeApiContentHelper.JSON_MIME_TYPE) {
      private val typeToken = TypeToken.get(clazz)

      override fun extractResult(response: GiteeApiResponse): T = parseJsonResponse(response, typeToken)
    }
  }

  open class Delete(url: String) : GiteeApiRequest<Unit>(url) {
    override val acceptMimeType: String? = null

    override fun extractResult(response: GiteeApiResponse) {}
  }

  companion object {
    private fun <T> parseJsonResponse(response: GiteeApiResponse, typeToken: TypeToken<T>): T {
      return response.readBody(ThrowableConvertor { GiteeApiContentHelper.readJson(it, typeToken) })
    }
  }
}
