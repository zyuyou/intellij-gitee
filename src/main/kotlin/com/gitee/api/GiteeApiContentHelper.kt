// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api

import com.google.common.io.ByteStreams
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import com.gitee.exceptions.GiteeFormUrlEncodedException
import com.gitee.exceptions.GiteeJsonException
import org.jetbrains.io.mandatory.Mandatory
import org.jetbrains.io.mandatory.NullCheckingFactory
import java.awt.Image
import java.awt.Toolkit
import java.io.IOException
import java.io.InputStream
import java.io.Reader
import kotlin.reflect.full.memberProperties

object GiteeApiContentHelper {
  const val JSON_MIME_TYPE = "application/json"
  const val FORM_URLENCODED_MINE_TYPE = "application/x-www-form-urlencoded"

//  const val V3_JSON_MIME_TYPE = "application/vnd.github.v3+json"
//  const val V3_HTML_JSON_MIME_TYPE = "application/vnd.github.v3.html+json"

  val gson: Gson = GsonBuilder()
    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
    .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
    .registerTypeAdapterFactory(NullCheckingFactory.INSTANCE)
    .create()

  @Throws(com.gitee.exceptions.GiteeJsonException::class)
  inline fun <reified T> fromJson(string: String): T = fromJson(string, T::class.java)

  @JvmStatic
  @Throws(com.gitee.exceptions.GiteeJsonException::class)
  fun <T> fromJson(string: String, clazz: Class<T>): T {
    try {
      return gson.fromJson(string, TypeToken.get(clazz).type)
    }
    catch (e: JsonParseException) {
      throw com.gitee.exceptions.GiteeJsonException("Couldn't parse Gitee response", e)
    }
  }

  @JvmStatic
  @Throws(com.gitee.exceptions.GiteeJsonException::class)
  fun <T> readJson(reader: Reader, typeToken: TypeToken<T>): T {
    try {
      return gson.fromJson(reader, typeToken.type)
    }
    catch (e: JsonParseException) {
      throw com.gitee.exceptions.GiteeJsonException("Couldn't parse Gitee response", e)
    }
  }

  @JvmStatic
  @Throws(com.gitee.exceptions.GiteeJsonException::class)
  fun toJson(content: Any): String {
    try {
      return gson.toJson(content)
    }
    catch (e: JsonIOException) {
      throw com.gitee.exceptions.GiteeJsonException("Couldn't serialize Gitee request body", e)
    }
  }

  @JvmStatic
  @Throws(com.gitee.exceptions.GiteeFormUrlEncodedException::class)
  fun toFormUrlEncoded(content: Any): String {
    try {
      return content.toString()
    }
    catch (e: JsonIOException) {
      throw com.gitee.exceptions.GiteeFormUrlEncodedException("Couldn't serialize Gitee request body", e)
    }
  }

  @JvmStatic
  @Throws(IOException::class)
  fun loadImage(stream: InputStream): Image {
    val bytes = ByteStreams.toByteArray(stream)
    return Toolkit.getDefaultToolkit().createImage(bytes)
  }
}