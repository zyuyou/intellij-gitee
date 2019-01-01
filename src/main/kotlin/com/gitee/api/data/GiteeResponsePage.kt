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
package com.gitee.api.data

import com.gitee.exceptions.GiteeConfusingException

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/api/data/GithubResponsePage.kt
 * @author JetBrains s.r.o.
 */
class GiteeResponsePage<T> constructor(private var totalCount: Int,
                                       var list: List<T>,
                                       val nextLink: String? = null) {

  val hasNext = nextLink != null

  companion object {
    @JvmStatic
    @Throws(GiteeConfusingException::class)
    fun <T> parseFromResult(responsePage: GiteeResponsePage<T>, requestUrl: String): GiteeResponsePage<T> {
      if (responsePage.list.size == responsePage.totalCount) return GiteeResponsePage(responsePage.totalCount, responsePage.list, null)

      if (responsePage.list.isEmpty()) return GiteeResponsePage(responsePage.totalCount, responsePage.list, null)

      val newNextLink = requestUrl.replace(Regex("([?&]+page=)(\\d+)")) {
        "${it.groupValues[1]}${it.groupValues[2].toInt() + 1}"
      }

      return GiteeResponsePage(responsePage.totalCount, responsePage.list, newNextLink)
    }

    @JvmStatic
    @Throws(GiteeConfusingException::class)
    fun <T> parseFromResult(list: List<T>, requestUrl: String): GiteeResponsePage<T> {
      if (list.isEmpty()) return GiteeResponsePage(0, list, null)

      val newNextLink = requestUrl.replace(Regex("([?&]+page=)(\\d+)")) {
        "${it.groupValues[1]}${it.groupValues[2].toInt() + 1}"
      }

      return GiteeResponsePage(0, list, newNextLink)
    }

    fun <T> empty(nextLink: String? = null) = GiteeResponsePage<T>(0, emptyList(), nextLink = nextLink)
  }
}


