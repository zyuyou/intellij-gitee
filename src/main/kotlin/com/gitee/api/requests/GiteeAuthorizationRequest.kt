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
package com.gitee.api.requests

import org.jetbrains.io.mandatory.Mandatory
import java.net.URLEncoder

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/api/requests/GithubAuthorizationCreateRequest.java
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/api/requests/GithubAuthorizationUpdateRequest.java
 * @author JetBrains s.r.o.
 */
sealed class GiteeAuthorizationRequest {
  companion object {
    @Mandatory
    const val GRANT_TYPE_CREATE_AUTH: String = "password"

    @Mandatory
    const val GRANT_TYPE_REFRESH_TOKEN: String = "refresh_token"
  }
}

data class AuthorizationCreateRequest(private val scopes: List<String>,
                         private val username: String,
                         private val password: String,
                         private val clientId: String? = "fc439d90cb2ffc20cffeb70a6a4039e69847485e0fa56cfa0d1bf006098e24dd",
                         private val clientSecret: String? = "386f187646ee361049f69cd213424bdba5af03e820d10a68a68e5fb520902596") : GiteeAuthorizationRequest() {

  override fun toString(): String {
    return listOf(
      "grant_type=$GRANT_TYPE_CREATE_AUTH",
      "scope=${scopes.joinToString(" ")}",
      "username=$username",
      "password=${URLEncoder.encode(password, "utf-8")}",
      "client_id=$clientId",
      "client_secret=$clientSecret"
    ).joinToString("&")
  }
}

data class AuthorizationUpdateRequest(private val refreshToken: String) : GiteeAuthorizationRequest() {

  override fun toString(): String {
    return listOf(
      "grant_type=$GRANT_TYPE_REFRESH_TOKEN",
      "refresh_token=$refreshToken"
    ).joinToString("&")
  }
}