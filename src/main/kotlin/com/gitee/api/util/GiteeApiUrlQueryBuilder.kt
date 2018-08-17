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
package com.gitee.api.util

import com.gitee.api.requests.GiteeRequestPagination

@DslMarker
private annotation class UrlQueryDsl

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/api/util/GithubApiUrlQueryBuilder.kt
 * @author JetBrains s.r.o.
 */
@UrlQueryDsl
class GiteeApiUrlQueryBuilder {
  private val builder = StringBuilder()

  fun param(name: String, value: String?) {
    if (value != null) append("$name=$value")
  }

  fun param(pagination: GiteeRequestPagination?) {
    if (pagination != null) {
      param("page", pagination.pageNumber.toString())
      param("per_page", pagination.pageSize.toString())
    }
  }

  private fun append(part: String) {
    if (builder.isEmpty()) builder.append("?") else builder.append("&")
    builder.append(part)
  }

  companion object {
    @JvmStatic
    fun urlQuery(init: GiteeApiUrlQueryBuilder.() -> Unit) : String {
      val query = GiteeApiUrlQueryBuilder()
      init(query)
      return query.builder.toString()
    }
  }
}