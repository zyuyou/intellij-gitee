// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.util

import com.gitee.api.GiteeApiRequest
import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.data.graphql.GiteeGQLPageInfo
import com.gitee.api.data.graphql.GiteeGQLRequestPagination
import com.gitee.api.data.request.GiteeRequestPagination
import com.intellij.openapi.progress.ProgressIndicator
import java.util.concurrent.atomic.AtomicReference

abstract class GiteeGQLPagesLoader<T, R>(private val executor: GiteeApiRequestExecutor,
                                         private val requestProducer: (GiteeGQLRequestPagination) -> GiteeApiRequest.Post<T>,
                                         private val pageSize: Int = GiteeRequestPagination.DEFAULT_PAGE_SIZE) {

  private val iterationDataRef = AtomicReference(IterationData(true))

  val hasNext: Boolean
    get() = iterationDataRef.get().hasNext

  @Synchronized
  fun loadNext(progressIndicator: ProgressIndicator): R? {
    val iterationData = iterationDataRef.get()
    if (!iterationData.hasNext) return null

    val response = executor.execute(progressIndicator, requestProducer(GiteeGQLRequestPagination(iterationData.cursor, pageSize)))
    val page = extractPageInfo(response)
    iterationDataRef.compareAndSet(iterationData, IterationData(page))

    return extractResult(response)
  }

  fun reset() {
    iterationDataRef.set(IterationData(true))
  }

  protected abstract fun extractPageInfo(result: T): GiteeGQLPageInfo
  protected abstract fun extractResult(result: T): R

  private class IterationData(val hasNext: Boolean, val cursor: String? = null) {
    constructor(page: GiteeGQLPageInfo) : this(page.hasNextPage, page.endCursor)
  }
}