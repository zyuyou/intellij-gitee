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

class GEApiCursorPageInfoDTO(val startCursor: Int?, val hasPreviousPage: Boolean,
                             val endCursor: Int?, val hasNextPage: Boolean)

class GEApiResponseDTO<D, E : GEApiErrorDTO>(val data: D?, val errors: List<E>?)

interface GEApiPagedResponseDataDTO<out T> {
  val pageInfo: GEApiCursorPageInfoDTO
  val items: List<T>
}

open class GEApiErrorDTO(val message: String/*,location, paths*/) {
  override fun toString(): String = message
}

class GEResponsePage<T> constructor(var items: List<T>,
                                    val nextLink: String? = null) {

  val hasNext = nextLink != null

  companion object {
//    const val HEADER_NAME = "Link"

    const val HEADER_TOTAL_COUNT = "total_count"
    const val HEADER_TOTAL_PAGE = "total_page"

    @JvmStatic
    @Throws(GiteeConfusingException::class)
    fun <T> parseFromHeader(items: List<T>, requestUrl: String, totalCountHeaderValue: Int?): GEResponsePage<T> {
      if (totalCountHeaderValue == null || items.size == totalCountHeaderValue) return GEResponsePage(items)

      val newNextLink = requestUrl.replace(Regex("([?&]+page=)(\\d+)")) {
        "${it.groupValues[1]}${it.groupValues[2].toInt() + 1}"
      }

      return GEResponsePage(items, newNextLink)
    }

    @JvmStatic
    @Throws(GiteeConfusingException::class)
    fun <T> parseFromHeaderPage(items: List<T>, requestUrl: String, totalPageHeaderValue: Int?): GEResponsePage<T> {
      val pageRegex = Regex("([?&]+page=)(\\d+)");
      val findPageResult = pageRegex.find(requestUrl);
      val curPage = findPageResult?.groupValues?.get(2)?.toInt()

      if (curPage == null || totalPageHeaderValue == 0 || curPage == totalPageHeaderValue) {
        return GEResponsePage(items)
      }

      val newNextLink = requestUrl.replace(Regex("([?&]+page=)(\\d+)")) {
        "${it.groupValues[1]}${it.groupValues[2].toInt() + 1}"
      }
      return GEResponsePage(items, newNextLink)
    }

    fun <T> empty(nextLink: String? = null) = GEResponsePage<T>(emptyList(), nextLink = nextLink)
  }
}


