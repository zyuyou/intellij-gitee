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

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.*
import com.fasterxml.jackson.databind.introspect.VisibilityChecker
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.gitee.exceptions.GiteeFormUrlEncodedException
import com.gitee.exceptions.GiteeJsonException
import java.awt.Image
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import java.text.SimpleDateFormat
import javax.imageio.ImageIO

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/api/GithubApiContentHelper.kt
 * @author JetBrains s.r.o.
 */
object GiteeApiContentHelper {
  const val JSON_MIME_TYPE = "application/json"
  const val FORM_URLENCODED_MINE_TYPE = "application/x-www-form-urlencoded"
  const val V3_HTML_JSON_MIME_TYPE = "application/vnd.gitee.html+json"
  const val V3_DIFF_JSON_MIME_TYPE = "application/vnd.gitee.diff+json"

  private val jackson: ObjectMapper = jacksonObjectMapper().genericConfig()
      .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)

  private val gqlJackson: ObjectMapper = jacksonObjectMapper().genericConfig()
      .setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE)

  private fun ObjectMapper.genericConfig(): ObjectMapper =
      this.setDateFormat(SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"))
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
          .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
          .setSerializationInclusion(JsonInclude.Include.NON_NULL)
          .setVisibility(VisibilityChecker.Std(
              JsonAutoDetect.Visibility.NONE,
              JsonAutoDetect.Visibility.NONE,
              JsonAutoDetect.Visibility.NONE,
              JsonAutoDetect.Visibility.NONE,
              JsonAutoDetect.Visibility.ANY
          ))

  @Throws(GiteeJsonException::class)
  inline fun <reified T> fromJson(string: String): T = fromJson(string, T::class.java)

  @JvmStatic
  @Throws(GiteeJsonException::class)
  fun <T> fromJson(string: String, clazz: Class<T>, gqlNaming: Boolean = false): T {
    try {
      return getObjectMapper(gqlNaming).readValue(string, clazz)
    }
    catch (e: com.fasterxml.jackson.core.JsonParseException) {
      throw GiteeJsonException("Can't parse Gitee response", e)
    }
  }

  @JvmStatic
  @Throws(GiteeJsonException::class)
  fun <T> readJsonObject(reader: Reader, clazz: Class<T>, vararg parameters: Class<*>, gqlNaming: Boolean = false): T {
    return readJson(reader, jackson.typeFactory.constructParametricType(clazz, *parameters), gqlNaming)
  }

  @JvmStatic
  @Throws(GiteeJsonException::class)
  fun <T> readJsonList(reader: Reader, parameterClass: Class<T>): List<T> {
    return readJson(reader, jackson.typeFactory.constructCollectionType(List::class.java, parameterClass))
  }

  @Throws(GiteeJsonException::class)
  private fun <T> readJson(reader: Reader, type: JavaType, gqlNaming: Boolean = false): T {
    try {
      @Suppress("UNCHECKED_CAST")
      if (type.isTypeOrSubTypeOf(Unit::class.java) || type.isTypeOrSubTypeOf(Void::class.java)) return Unit as T
      return getObjectMapper(gqlNaming).readValue(reader, type)
    }
    catch (e: JsonProcessingException) {
      throw GiteeJsonException("Can't parse Gitee response", e)
    }
  }

  @JvmStatic
  @Throws(GiteeJsonException::class)
  fun toJson(content: Any, gqlNaming: Boolean = false): String {
    try {
      return getObjectMapper(gqlNaming).writeValueAsString(content)
    }
    catch (e: JsonProcessingException) {
      throw GiteeJsonException("Can't serialize Gitee request body", e)
    }
  }

  // for Gitee
  @JvmStatic
  @Throws(GiteeFormUrlEncodedException::class)
  fun toFormUrlEncoded(content: Any): String {
    return content.toString()
  }

  private fun getObjectMapper(gqlNaming: Boolean = false): ObjectMapper = if (!gqlNaming) jackson else gqlJackson

  @JvmStatic
  @Throws(IOException::class)
  fun loadImage(stream: InputStream): Image {
    return ImageIO.read(stream)
  }
}