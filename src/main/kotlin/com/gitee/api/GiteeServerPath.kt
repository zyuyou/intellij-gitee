package com.gitee.api

import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.io.URLUtil
import com.intellij.util.xmlb.annotations.Attribute
import com.intellij.util.xmlb.annotations.Tag
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
                                                     val suffix: String? = null) {

  companion object {
    const val DEFAULT_HOST: String = "gitee.com";

    //        private val API_PREFIX: String = "api.";
    private const val API_SUFFIX: String = "/api/v5";
    private const val ENTERPRISE_API_SUFFIX: String = "/api/v5";

    private val URL_REGEX = Pattern.compile("^(https?://)?([^/?:]+)(:(\\d+))?((/[^/?#]+)*)?/?",
      Pattern.CASE_INSENSITIVE)

    @Throws(com.gitee.exceptions.GiteeParseException::class)
    fun from(uri: String): GiteeServerPath {
      val matcher: Matcher = URL_REGEX.matcher(uri);

      if (!matcher.matches()) throw com.gitee.exceptions.GiteeParseException("Not a valid URL");

      val schema: String? = matcher.group(1);
      val httpSchema: Boolean? = if (schema == null || schema.isEmpty()) null else schema.equals("http://", true);

      val host: String = matcher.group(2) ?: throw com.gitee.exceptions.GiteeParseException("Empty host");

      val portGroup: String? = matcher.group(4);
      val port: Int? = if (portGroup == null) {
        null
      } else {
        try {
          portGroup.toInt();
        } catch (ignore: NumberFormatException) {
          throw com.gitee.exceptions.GiteeParseException("Invalid port format")
        }
      }

      val suffix: String? = StringUtil.nullize(matcher.group(5));

      return GiteeServerPath(httpSchema, host, port, suffix)
    }
  }

  fun getSchema(): String {
    return if (useHttp == null || !useHttp) "https" else "http";
  }

  fun matches(gitRemoteUrl: String): Boolean {
    val url = com.gitee.util.GiteeUrlUtil.removePort(com.gitee.util.GiteeUrlUtil.removeProtocolPrefix(gitRemoteUrl))
    return StringUtil.startsWithIgnoreCase(url, host + StringUtil.notNullize(suffix))
  }

  fun toUrl(): String {
    return getSchemaUrlPart() + host + getPortUrlPart() + StringUtil.notNullize(suffix)
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

  private fun getPortUrlPart(): String {
    return if (port != null) (":" + port.toString()) else ""
  }

  private fun getSchemaUrlPart(): String {
    return getSchema() + URLUtil.SCHEME_SEPARATOR
  }

  override fun toString(): String {
    val schema = if (useHttp != null) getSchemaUrlPart() else ""
    return schema + host + getPortUrlPart() + StringUtil.notNullize(suffix)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is GiteeServerPath) return false

    val path = other as GiteeServerPath?

    return useHttp == path!!.useHttp
      && host == path.host
      && port == path.port
      && suffix == path.suffix
  }

  override fun hashCode(): Int {
    return Objects.hash(useHttp, host, port, suffix)
  }

}