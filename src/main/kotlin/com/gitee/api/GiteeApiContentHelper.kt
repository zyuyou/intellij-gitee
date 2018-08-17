/*
 * Copyright 2016-2018 码云 - Gitee
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gitee.api

import com.gitee.exceptions.GiteeFormUrlEncodedException
import com.gitee.exceptions.GiteeJsonException
import com.google.common.io.ByteStreams
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import org.jetbrains.io.mandatory.NullCheckingFactory
import java.awt.Image
import java.awt.Toolkit
import java.io.IOException
import java.io.InputStream
import java.io.Reader

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/api/GithubApiContentHelper.kt
 * @author JetBrains s.r.o.
 */
object GiteeApiContentHelper {
  const val JSON_MIME_TYPE = "application/json"
  const val FORM_URLENCODED_MINE_TYPE = "application/x-www-form-urlencoded"

  private val gson: Gson = GsonBuilder()
    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .registerTypeAdapterFactory(NullCheckingFactory.INSTANCE)
    .create()

  @Throws(GiteeJsonException::class)
  inline fun <reified T> fromJson(string: String): T = fromJson(string, T::class.java)

  @JvmStatic
  @Throws(GiteeJsonException::class)
  fun <T> fromJson(string: String, clazz: Class<T>): T {
    try {
      return gson.fromJson(string, TypeToken.get(clazz).type)
    }
    catch (e: JsonParseException) {
      throw GiteeJsonException("Couldn't parse Gitee response", e)
    }
  }

  @JvmStatic
  @Throws(GiteeJsonException::class)
  fun <T> readJson(reader: Reader, typeToken: TypeToken<T>): T {
    try {
      return gson.fromJson(reader, typeToken.type)
    }
    catch (e: JsonParseException) {
      throw GiteeJsonException("Couldn't parse Gitee response", e)
    }
  }

  @JvmStatic
  @Throws(GiteeJsonException::class)
  fun toJson(content: Any): String {
    try {
      return gson.toJson(content)
    }
    catch (e: JsonIOException) {
      throw GiteeJsonException("Couldn't serialize Gitee request body", e)
    }
  }

  @JvmStatic
  @Throws(GiteeFormUrlEncodedException::class)
  fun toFormUrlEncoded(content: Any): String {
    try {
      return content.toString()
    }
    catch (e: JsonIOException) {
      throw GiteeFormUrlEncodedException("Couldn't serialize Gitee request body", e)
    }
  }

  @JvmStatic
  @Throws(IOException::class)
  fun loadImage(stream: InputStream): Image {
    val bytes = ByteStreams.toByteArray(stream)
    return Toolkit.getDefaultToolkit().createImage(bytes)
  }
}