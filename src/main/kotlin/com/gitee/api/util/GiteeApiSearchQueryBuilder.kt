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

package com.gitee.api.util

@DslMarker
private annotation class SearchQueryDsl

@SearchQueryDsl
class GiteeApiSearchQueryBuilder {
  private val builder = StringBuilder()

  fun qualifier(name: String, value: String?) {
    if (value != null) append("$name:$value")
  }

  fun query(value: String?) {
    if (value != null) append(value)
  }

  private fun append(part: String) {
    if (builder.isNotEmpty()) builder.append(" ")
    builder.append(part)
  }

  companion object {
    @JvmStatic
    fun searchQuery(init: GiteeApiSearchQueryBuilder.() -> Unit): String {
      val query = GiteeApiSearchQueryBuilder()
      init(query)
      return query.builder.toString()
    }
  }
}