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

import com.gitee.api.GiteeApiRequest
import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.data.GiteeResponsePage
import com.intellij.openapi.progress.ProgressIndicator
import java.io.IOException
import java.util.function.Predicate

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/api/util/GithubApiPagesLoader.kt
 * @author JetBrains s.r.o.
 */
object GiteeApiPagesLoader {

  @Throws(IOException::class)
  @JvmStatic
  fun <T> loadAll(executor: GiteeApiRequestExecutor, indicator: ProgressIndicator, pagesRequest: Request<T>): List<T> {
    val result = mutableListOf<T>()
    var request: GiteeApiRequest<GiteeResponsePage<T>>? = pagesRequest.initialRequest

    while (request != null) {
      val page = executor.execute(indicator, request)
      result.addAll(page.list)
      request = page.nextLink?.let(pagesRequest.urlRequestProvider)
    }
    return result
  }

  @Throws(IOException::class)
  @JvmStatic
  fun <T> find(executor: GiteeApiRequestExecutor, indicator: ProgressIndicator, pagesRequest: Request<T>, predicate: Predicate<T>): T? {
    var request: GiteeApiRequest<GiteeResponsePage<T>>? = pagesRequest.initialRequest
    while (request != null) {
      val page = executor.execute(indicator, request)
      page.list.find { predicate.test(it) }?.let { return it }
      request = page.nextLink?.let(pagesRequest.urlRequestProvider)
    }
    return null
  }

  @Throws(IOException::class)
  @JvmStatic
  fun <T> load(executor: GiteeApiRequestExecutor, indicator: ProgressIndicator, pagesRequest: Request<T>, maximum: Int): List<T> {
    val result = mutableListOf<T>()
    var request: GiteeApiRequest<GiteeResponsePage<T>>? = pagesRequest.initialRequest

    while (request != null) {
      val page = executor.execute(indicator, request)

      for (item in page.list) {
        result.add(item)
        if (result.size == maximum) return result
      }

      request = page.nextLink?.let(pagesRequest.urlRequestProvider)
    }
    return result
  }

  class Request<T>(val initialRequest: GiteeApiRequest<GiteeResponsePage<T>>,
                   val urlRequestProvider: (String) -> GiteeApiRequest<GiteeResponsePage<T>>)
}