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

import com.gitee.exceptions.GiteeParseException
import com.intellij.collaboration.api.ServerPath
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.io.URLUtil
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
import git4idea.remote.hosting.GitHostingUrlUtil
import org.jetbrains.annotations.NotNull
import java.net.URI
import java.net.URISyntaxException
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

/**
 * @author Yuyou Chow
 *
 * Github server reference allowing to specify custom port and path to instance
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/api/GithubServerPath.java
 * @author JetBrains s.r.o.
 */
@Tag("Server")
data class GiteeServerPath @JvmOverloads constructor(@field:Attribute("useHttp")
                                                     val useHttp: Boolean? = null,
                                                     @field:Attribute("host")
                                                     val host: String = "",
                                                     @field:Attribute("port")
                                                     val port: Int? = null,
                                                     @field:Attribute("suffix")
                                                     val suffix: String? = null,
                                                     @field:Attribute("clientid")
                                                     val clientId: String? = null,
                                                     @field:Attribute("clientsecret")
                                                     val clientSecret: String? = null) : ServerPath {

  companion object {
    const val DEFAULT_HOST: String = "gitee.com"

    val DEFAULT_SERVER = GiteeServerPath(host = DEFAULT_HOST)

    private const val API_SUFFIX: String = "/api/v5"
    private const val ENTERPRISE_API_SUFFIX: String = "/api/v5"

    private val URL_REGEX = Pattern.compile("^(https?://)?([^/?:]+)(:(\\d+))?((/[^/?#]+)*)?/?", Pattern.CASE_INSENSITIVE)

    @Throws(GiteeParseException::class)
    fun from(uri: String, clientId: String? = null, clientSecret: String? = null): GiteeServerPath {
      val matcher: Matcher = URL_REGEX.matcher(uri)

      if (!matcher.matches()) throw GiteeParseException("Not a valid URL")

      val schema: String? = matcher.group(1)
      val httpSchema: Boolean? = if (schema.isNullOrEmpty()) null else schema.equals("http://", true)

      val host: String = matcher.group(2) ?: throw GiteeParseException("Empty host")

      val portGroup: String? = matcher.group(4)
      val port: Int? = if (portGroup == null) {
        null
      } else {
        try {
          portGroup.toInt()
        } catch (ignore: NumberFormatException) {
          throw GiteeParseException("Invalid port format")
        }
      }

      val suffix: String? = StringUtil.nullize(matcher.group(5))

      return GiteeServerPath(httpSchema, host, port, suffix, clientId, clientSecret)
    }
  }

  fun getSchema(): String {
    return if (useHttp == null || !useHttp) "https" else "http"
  }

  fun matches(gitRemoteUrl: String): Boolean {
    return GitHostingUrlUtil.match(toURI(), gitRemoteUrl)
  }

  fun toUrl(): String {
    return toUrl(true)
  }

  fun toHostUrl(): String {
    return getSchemaUrlPart() + host + getPortUrlPart()
  }

  @NotNull
  fun toUrl(showSchema: Boolean): String {
    val builder = StringBuilder()
    if (showSchema) builder.append(getSchemaUrlPart())
    builder.append(host).append(getPortUrlPart()).append(StringUtil.notNullize(suffix))
    return builder.toString()
  }

  fun toApiUrl(): String {
    val builder = StringBuilder(getSchemaUrlPart())

    if (host.equals(DEFAULT_HOST, true)) {
      builder.append(host).append(getPortUrlPart()).append(API_SUFFIX).append(StringUtil.notNullize(suffix))
    } else {
      builder.append(host).append(getPortUrlPart()).append(StringUtil.notNullize(suffix)).append(ENTERPRISE_API_SUFFIX)
    }
    return builder.toString()
  }

  fun toGraphQLUrl(): String {
    return ""
  }

  fun isGiteeDotCom(): Boolean {
    return host.equals(DEFAULT_HOST, true)
  }

  private fun getPortUrlPart(): String {
    return if (port != null) (":$port") else ""
  }

  private fun getSchemaUrlPart(): String {
    return getSchema() + URLUtil.SCHEME_SEPARATOR
  }

  override fun toString(): String {
    val schema = if (useHttp != null) getSchemaUrlPart() else ""
    return schema + host + getPortUrlPart() + StringUtil.notNullize(suffix)
  }

  override fun toURI(): URI {
    val port = port ?: -1

    return try {
      URI(getSchema(), null, this.host, port, suffix, null, null)
    } catch (e: URISyntaxException) {
      // shouldn't happen, because we pre-validate the data
      throw RuntimeException(e)
    }
  }

  override fun equals(other: Any?): Boolean {
    return equals(other, false);
  }

  fun equals(o: Any?, ignoreProtocol: Boolean): Boolean {
    if (this === o) return true
    if (o !is GiteeServerPath) return false

    val path: GiteeServerPath = o

    return (ignoreProtocol || useHttp == path.useHttp) &&
      host == path.host &&
      port == path.port &&
      suffix == path.suffix
  }

  override fun hashCode(): Int {
    return Objects.hash(useHttp, host, port, suffix)
  }

}