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

import com.fasterxml.jackson.databind.JsonNode
import com.gitee.api.data.GiteeResponsePage
import com.gitee.api.data.GiteeSearchResult
import com.gitee.api.data.graphql.GiteeGQLQueryRequest
import com.gitee.api.data.graphql.GiteeGQLResponse
import com.gitee.exceptions.GiteeConfusingException
import com.gitee.exceptions.GiteeJsonException
import com.intellij.util.ThrowableConvertor
import java.io.IOException

/**
 * Represents an API request with strictly defined response type
 *
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/api/GiteeApiRequest.kt
 * @author JetBrains s.r.o.
 */
sealed class GiteeApiRequest<out T>(val url: String) {
  var operationName: String? = null
  abstract val acceptMimeType: String?

  open val tokenHeaderType = GiteeApiRequestExecutor.TokenHeaderType.TOKEN

  protected val headers = mutableMapOf<String, String>()
  val additionalHeaders: Map<String, String>
    get() = headers

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
        inline fun <reified T> json(url: String, acceptMimeType: String? = null): Optional<T> =
            Json(url, T::class.java, acceptMimeType)
      }

      open class Json<T>(url: String, private val clazz: Class<T>, acceptMimeType: String? = GiteeApiContentHelper.JSON_MIME_TYPE)
        : Optional<T>(url, acceptMimeType) {

        override fun extractResult(response: GiteeApiResponse): T = parseJsonObject(response, clazz)
      }
    }

    companion object {
      inline fun <reified T> json(url: String, acceptMimeType: String? = null): Get<T> =
          Json(url, T::class.java, acceptMimeType)

      inline fun <reified T> jsonPage(url: String, acceptMimeType: String? = null): Get<GiteeResponsePage<T>> =
          JsonPage(url, T::class.java, acceptMimeType)

      inline fun <reified T> jsonSearchPage(url: String, acceptMimeType: String? = null): Get<GiteeResponsePage<T>> =
          JsonSearchPage(url, T::class.java, acceptMimeType)

      // requests for Gitee
      inline fun <reified T> jsonList(url: String): Get<List<T>> = JsonList(url, T::class.java)

    }

    open class Json<T>(url: String, private val clazz: Class<T>, acceptMimeType: String? = GiteeApiContentHelper.JSON_MIME_TYPE)
      : Get<T>(url, acceptMimeType) {

      override fun extractResult(response: GiteeApiResponse): T = parseJsonObject(response, clazz)
    }

    open class JsonList<T>(url: String, private val clazz: Class<T>, acceptMimeType: String? = GiteeApiContentHelper.JSON_MIME_TYPE)
      : Get<List<T>>(url, acceptMimeType) {

      override fun extractResult(response: GiteeApiResponse): List<T> = parseJsonList(response, clazz)
    }

    open class JsonPage<T>(url: String, private val clazz: Class<T>, acceptMimeType: String? = GiteeApiContentHelper.JSON_MIME_TYPE)
      : Get<GiteeResponsePage<T>>(url, acceptMimeType) {

      override fun extractResult(response: GiteeApiResponse): GiteeResponsePage<T> {
        return GiteeResponsePage.parseFromHeader(parseJsonList(response, clazz), url,
            response.findHeader(GiteeResponsePage.HEADER_TOTAL_COUNT)?.toInt())
      }
    }

    open class JsonSearchPage<T>(url: String,
                                 private val clazz: Class<T>,
                                 acceptMimeType: String? = GiteeApiContentHelper.JSON_MIME_TYPE)
      : Get<GiteeResponsePage<T>>(url, acceptMimeType) {

      override fun extractResult(response: GiteeApiResponse): GiteeResponsePage<T> {
        return GiteeResponsePage.parseFromHeader(parseJsonSearchPage(response, clazz).items, url,
            response.findHeader(GiteeResponsePage.HEADER_TOTAL_COUNT)?.toInt())
      }
    }
  }

  abstract class Head<T> @JvmOverloads constructor(url: String,
                                                   override val acceptMimeType: String? = null) : GiteeApiRequest<T>(url)

  abstract class WithBody<out T>(url: String) : GiteeApiRequest<T>(url) {
    abstract val body: String?
    abstract val bodyMimeType: String
  }

  abstract class Post<out T> @JvmOverloads constructor(override val bodyMimeType: String,
                                                       url: String,
                                                       override val acceptMimeType: String? = null) : GiteeApiRequest.WithBody<T>(url) {
    companion object {
      inline fun <reified T> json(url: String, body: Any, acceptMimeType: String? = null): Post<T> =
          Json(url, body, T::class.java, acceptMimeType)

      inline fun <reified T> formUrlEncoded(url: String, body: Any, acceptMimeType: String? = null): Post<T> =
          FormUrlEncoded(url, body, T::class.java, acceptMimeType)
    }

    open class Json<T>(url: String, private val bodyObject: Any, private val clazz: Class<T>,
                       acceptMimeType: String? = GiteeApiContentHelper.JSON_MIME_TYPE)
      : Post<T>(GiteeApiContentHelper.JSON_MIME_TYPE, url, acceptMimeType) {

      override val body: String
        get() = GiteeApiContentHelper.toJson(bodyObject)

      override fun extractResult(response: GiteeApiResponse): T = parseJsonObject(response, clazz)
    }

    open class FormUrlEncoded<T>(url: String, private val bodyObject: Any, private val clazz: Class<T>,
                                 acceptMimeType: String? = GiteeApiContentHelper.JSON_MIME_TYPE)
      : Post<T>(GiteeApiContentHelper.FORM_URLENCODED_MINE_TYPE, url, acceptMimeType) {

      override val body: String
        get() = GiteeApiContentHelper.toFormUrlEncoded(bodyObject)

      override fun extractResult(response: GiteeApiResponse): T = parseJsonObject(response, clazz)
    }

    abstract class GQLQuery<out T>(url: String,
                                   private val queryName: String,
                                   private val variablesObject: Any)
      : Post<T>(GiteeApiContentHelper.JSON_MIME_TYPE, url) {

      override val tokenHeaderType = GiteeApiRequestExecutor.TokenHeaderType.BEARER

      override val body: String
        get() {
          val query = GiteeGQLQueryLoader.loadQuery(queryName)
          val request = GiteeGQLQueryRequest(query, variablesObject)
          return GiteeApiContentHelper.toJson(request, true)
        }

      class Parsed<out T>(url: String,
                          requestFilePath: String,
                          variablesObject: Any,
                          private val clazz: Class<T>)
        : GQLQuery<T>(url, requestFilePath, variablesObject) {
        override fun extractResult(response: GiteeApiResponse): T {
          val result: GiteeGQLResponse<out T> = parseGQLResponse(response, clazz)
          val data = result.data
          if (data != null) return data
          else throw GiteeConfusingException(result.errors.toString())
        }
      }

      class TraversedParsed<out T : Any>(url: String,
                                         requestFilePath: String,
                                         variablesObject: Any,
                                         private val clazz: Class<out T>,
                                         private vararg val pathFromData: String)
        : GQLQuery<T>(url, requestFilePath, variablesObject) {

        override fun extractResult(response: GiteeApiResponse): T {
          return parseResponse(response, clazz, pathFromData)
              ?: throw GiteeJsonException("Non-nullable entity is null or entity path is invalid")
        }
      }

      class OptionalTraversedParsed<T>(url: String,
                                       requestFilePath: String,
                                       variablesObject: Any,
                                       private val clazz: Class<T>,
                                       private vararg val pathFromData: String)
        : GQLQuery<T?>(url, requestFilePath, variablesObject) {
        override fun extractResult(response: GiteeApiResponse): T? {
          return parseResponse(response, clazz, pathFromData)
        }
      }

      internal fun <T> parseResponse(response: GiteeApiResponse,
                                     clazz: Class<T>,
                                     pathFromData: Array<out String>): T? {
        val result: GiteeGQLResponse<out JsonNode> = parseGQLResponse(response, JsonNode::class.java)
        val data = result.data
        if (data != null && !data.isNull) {
          var node: JsonNode = data
          for (path in pathFromData) {
            node = node[path] ?: break
          }
          if (!node.isNull) return GiteeApiContentHelper.fromJson(node.toString(), clazz, true)
        }
        val errors = result.errors
        if (errors != null) throw GiteeConfusingException(errors.toString()) else return null
      }
    }
  }

  abstract class Put<T> @JvmOverloads constructor(override val bodyMimeType: String,
                                                  url: String,
                                                  override val acceptMimeType: String? = null) : GiteeApiRequest.WithBody<T>(url) {
    companion object {
      inline fun <reified T> json(url: String, body: Any? = null): Put<T> = Json(url, body, T::class.java)

      inline fun <reified T> jsonList(url: String, body: Any): Put<List<T>> = JsonList(url, body, T::class.java)
    }

    open class Json<T>(url: String, private val bodyObject: Any?, private val clazz: Class<T>)
      : Put<T>(GiteeApiContentHelper.JSON_MIME_TYPE, url, GiteeApiContentHelper.JSON_MIME_TYPE) {
      init {
        if (bodyObject == null) headers["Content-Length"] = "0"
      }

      override val body: String?
        get() = bodyObject?.let { GiteeApiContentHelper.toJson(it) }

      override fun extractResult(response: GiteeApiResponse): T = parseJsonObject(response, clazz)
    }

    open class JsonList<T>(url: String, private val bodyObject: Any?, private val clazz: Class<T>)
      : Put<List<T>>(GiteeApiContentHelper.JSON_MIME_TYPE, url, GiteeApiContentHelper.JSON_MIME_TYPE) {
      init {
        if (bodyObject == null) headers["Content-Length"] = "0"
      }

      override val body: String?
        get() = bodyObject?.let { GiteeApiContentHelper.toJson(it) }

      override fun extractResult(response: GiteeApiResponse): List<T> = parseJsonList(response, clazz)
    }
  }

  abstract class Patch<T> @JvmOverloads constructor(override val bodyMimeType: String,
                                                    url: String,
                                                    override val acceptMimeType: String? = null)
    : GiteeApiRequest.WithBody<T>(url) {

    companion object {
      inline fun <reified T> json(url: String, body: Any): Patch<T> = Json(url, body, T::class.java)
    }

    open class Json<T>(url: String, private val bodyObject: Any, private val clazz: Class<T>)
      : Patch<T>(GiteeApiContentHelper.JSON_MIME_TYPE, url, GiteeApiContentHelper.JSON_MIME_TYPE) {

      override val body: String?
        get() = bodyObject.let { GiteeApiContentHelper.toJson(it) }

      override fun extractResult(response: GiteeApiResponse): T = parseJsonObject(response, clazz)
    }
  }

  abstract class Delete<T> @JvmOverloads constructor(override val bodyMimeType: String,
                                                     url: String,
                                                     override val acceptMimeType: String? = null) : GiteeApiRequest.WithBody<T>(url) {

    companion object {
      inline fun <reified T> json(url: String, body: Any? = null): Delete<T> = Json(url, body, T::class.java)
    }

    open class Json<T>(url: String, private val bodyObject: Any? = null, private val clazz: Class<T>)
      : Delete<T>(GiteeApiContentHelper.JSON_MIME_TYPE, url, GiteeApiContentHelper.JSON_MIME_TYPE) {
      init {
        if (bodyObject == null) headers["Content-Length"] = "0"
      }

      override val body: String?
        get() = bodyObject?.let { GiteeApiContentHelper.toJson(it) }

      override fun extractResult(response: GiteeApiResponse): T = parseJsonObject(response, clazz)
    }
  }

  companion object {
    private fun <T> parseJsonObject(response: GiteeApiResponse, clazz: Class<T>): T {
      return response.readBody(ThrowableConvertor { GiteeApiContentHelper.readJsonObject(it, clazz) })
    }

    private fun <T> parseJsonList(response: GiteeApiResponse, clazz: Class<T>): List<T> {
      return response.readBody(ThrowableConvertor { GiteeApiContentHelper.readJsonList(it, clazz) })
    }

    private fun <T> parseJsonSearchPage(response: GiteeApiResponse, clazz: Class<T>): GiteeSearchResult<T> {
      return response.readBody(ThrowableConvertor {
        @Suppress("UNCHECKED_CAST")
        GiteeApiContentHelper.readJsonObject(it, GiteeSearchResult::class.java, clazz) as GiteeSearchResult<T>
      })
    }

    private fun <T> parseGQLResponse(response: GiteeApiResponse, clazz: Class<out T>): GiteeGQLResponse<out T> {
      return response.readBody(ThrowableConvertor {
        @Suppress("UNCHECKED_CAST")
        GiteeApiContentHelper.readJsonObject(it, GiteeGQLResponse::class.java, clazz, gqlNaming = true) as GiteeGQLResponse<T>
      })
    }
  }
}
