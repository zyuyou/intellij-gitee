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
class GiteeResponsePage<T> constructor(var items: List<T>,
                                       val nextLink: String? = null) {

  val hasNext = nextLink != null

  companion object {
//    const val HEADER_NAME = "Link"

    const val HEADER_TOTAL_COUNT = "total_count"
    const val HEADER_TOTAL_PAGE = "total_page"

    @JvmStatic
    @Throws(GiteeConfusingException::class)
    fun <T> parseFromHeader(items: List<T>, requestUrl: String, totalCountHeaderValue: Int?): GiteeResponsePage<T> {
      if (totalCountHeaderValue == null || items.size == totalCountHeaderValue) return GiteeResponsePage(items)

      val newNextLink = requestUrl.replace(Regex("([?&]+page=)(\\d+)")) {
        "${it.groupValues[1]}${it.groupValues[2].toInt() + 1}"
      }

      return GiteeResponsePage(items, newNextLink)
    }

    @JvmStatic
    @Throws(GiteeConfusingException::class)
    fun <T> parseFromHeaderPage(items: List<T>, requestUrl: String, totalPageHeaderValue: Int?): GiteeResponsePage<T> {
      val curPage = Regex("([?&]+page=)(\\d+)").find(requestUrl)?.groupValues?.get(2)?.toInt()
      if(curPage == null || curPage == totalPageHeaderValue) return GiteeResponsePage(items)

      val newNextLink = requestUrl.replace(Regex("([?&]+page=)(\\d+)")) {
        "${it.groupValues[1]}${it.groupValues[2].toInt() + 1}"
      }

      return GiteeResponsePage(items, newNextLink)
    }

    fun <T> empty(nextLink: String? = null) = GiteeResponsePage<T>(emptyList(), nextLink = nextLink)
  }
}


