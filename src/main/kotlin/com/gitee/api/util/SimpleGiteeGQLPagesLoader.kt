// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.util

import com.gitee.api.GiteeApiRequest
import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.data.graphql.GiteeGQLPagedRequestResponse
import com.gitee.api.data.graphql.GiteeGQLRequestPagination
import com.gitee.api.data.request.GiteeRequestPagination
import com.intellij.openapi.progress.ProgressIndicator

class SimpleGiteeGQLPagesLoader<T>(executor: GiteeApiRequestExecutor,
                                   requestProducer: (GiteeGQLRequestPagination) -> GiteeApiRequest.Post<GiteeGQLPagedRequestResponse<T>>,
                                   pageSize: Int = GiteeRequestPagination.DEFAULT_PAGE_SIZE)
  : GiteeGQLPagesLoader<GiteeGQLPagedRequestResponse<T>, List<T>>(executor, requestProducer, pageSize) {

  fun loadAll(progressIndicator: ProgressIndicator): List<T> {
    val list = mutableListOf<T>()
    while (hasNext) {
      loadNext(progressIndicator)?.let { list.addAll(it) }
    }
    return list
  }

  override fun extractPageInfo(result: GiteeGQLPagedRequestResponse<T>) = result.pageInfo

  override fun extractResult(result: GiteeGQLPagedRequestResponse<T>) = result.nodes
}