// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data

class GiteeResponsePage<T> constructor(private var totalCount: Int,
                                       var list: List<T>,
                                       val nextLink: String? = null) {

  companion object {
    @JvmStatic
    @Throws(com.gitee.exceptions.GiteeConfusingException::class)
    fun <T> parseFromResult(responsePage: GiteeResponsePage<T>, requestUrl: String): GiteeResponsePage<T> {
      if (responsePage.list.size == responsePage.totalCount) return GiteeResponsePage(responsePage.totalCount, responsePage.list, null)

      if (responsePage.list.isEmpty()) return GiteeResponsePage(responsePage.totalCount, responsePage.list, null)

      val newNextLink = requestUrl.replace(Regex("([?&]+page=)(\\d+)")) {
        "${it.groupValues[1]}${it.groupValues[2].toInt() + 1}"
      }

      return GiteeResponsePage(responsePage.totalCount, responsePage.list, newNextLink)
    }

    @JvmStatic
    @Throws(com.gitee.exceptions.GiteeConfusingException::class)
    fun <T> parseFromResult(list: List<T>, requestUrl: String): GiteeResponsePage<T> {
      if (list.isEmpty()) return GiteeResponsePage(0, list, null)

      val newNextLink = requestUrl.replace(Regex("([?&]+page=)(\\d+)")) {
        "${it.groupValues[1]}${it.groupValues[2].toInt() + 1}"
      }

      return GiteeResponsePage(0, list, newNextLink)
    }
  }
}


