// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.util

import com.gitee.api.GiteeApiRequest
import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.data.GiteeResponsePage
import com.gitee.api.data.request.GiteeRequestPagination
import com.intellij.openapi.progress.ProgressIndicator

class SimpleGiteeFakeGQLPagesLoader<T>(executor: GiteeApiRequestExecutor,
                                       requestProducer: (GiteeRequestPagination) -> GiteeApiRequest<GiteeResponsePage<T>>,
                                       pageSize: Int = GiteeRequestPagination.DEFAULT_PAGE_SIZE)
  : GiteeFakeGQLPagesLoader<GiteeResponsePage<T>, List<T>>(executor, requestProducer, pageSize) {

  fun loadAll(progressIndicator: ProgressIndicator): List<T> {
    val list = mutableListOf<T>()
    while (hasNext) {
      loadNext(progressIndicator)?.let { list.addAll(it) }
    }
    return list
  }

  override fun extractHasNext(result: GiteeResponsePage<T>) = result.hasNext
  override fun extractNextLink(result: GiteeResponsePage<T>) = result.nextLink
  override fun extractItems(result: GiteeResponsePage<T>) = result.items

//  override fun extractPageInfo(result: GiteeResponsePage<T>) = result.pageInfo
//  override fun extractResult(result: GiteeResponsePage<T>) = result.nodes
}