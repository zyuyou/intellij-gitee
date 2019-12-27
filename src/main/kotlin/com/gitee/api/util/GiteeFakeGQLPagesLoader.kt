// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.util

import com.gitee.api.GiteeApiRequest
import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.data.request.GiteeRequestPagination
import com.intellij.openapi.progress.ProgressIndicator
import java.util.concurrent.atomic.AtomicReference

abstract class GiteeFakeGQLPagesLoader<T, R>(private val executor: GiteeApiRequestExecutor,
                                             private val requestProducer: (GiteeRequestPagination) -> GiteeApiRequest<T>,
                                             private val pageSize: Int = GiteeRequestPagination.DEFAULT_PAGE_SIZE) {

  private val iterationDataRef = AtomicReference(IterationData(true))

  val hasNext: Boolean
    get() = iterationDataRef.get().hasNext

  @Synchronized
  fun loadNext(progressIndicator: ProgressIndicator): R? {
    val iterationData = iterationDataRef.get()
    if (!iterationData.hasNext) return null

    val pageNumber: Int =
      if (iterationDataRef.get().nextLink == null)
        1
      else
        Regex("([?&]+page=)(\\d+)").matchEntire(iterationDataRef.get().nextLink!!)!!.let { it.groupValues[2].toInt() + 1 }

    val response = executor.execute(progressIndicator, requestProducer(GiteeRequestPagination(pageNumber, pageSize)))
    iterationDataRef.compareAndSet(iterationData, IterationData(extractHasNext(response), extractNextLink(response)))

    return extractItems(response)
  }

  fun reset() {
    iterationDataRef.set(IterationData(true))
  }

  protected abstract fun extractHasNext(result: T): Boolean
  protected abstract fun extractNextLink(result: T): String?
  protected abstract fun extractItems(result: T): R
//  protected abstract fun extractPageInfo(result: T): GiteeGQLPageInfo
//  protected abstract fun extractResult(result: T): R

  private class IterationData(val hasNext: Boolean, val nextLink: String? = null) {
//    constructor(page: GiteeResponsePage) : this(page.hasNextPage, page.endCursor)
  }
}