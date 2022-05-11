// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.util

import com.gitee.api.GiteeApiRequest
import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.data.GEApiCursorPageInfoDTO
import com.gitee.api.data.request.GEApiRequestPagination
import com.gitee.api.data.request.GiteeRequestPagination
import com.intellij.openapi.progress.ProgressIndicator
import java.util.*
import java.util.concurrent.atomic.AtomicReference

abstract class GEApiPagesLoader<T, R>(private val executor: GiteeApiRequestExecutor,
                                      private val requestProducer: (GEApiRequestPagination) -> GiteeApiRequest.Get<T>,
                                      private val supportsTimestampUpdates: Boolean = false,
                                      private val pageSize: Int = GiteeRequestPagination.DEFAULT_PAGE_SIZE) {

  private val iterationDataRef = AtomicReference(IterationData(true))

  val hasNext: Boolean
    get() = iterationDataRef.get().hasNext

  @Synchronized
  fun loadNext(progressIndicator: ProgressIndicator, update: Boolean = false): R? {
    val iterationData = iterationDataRef.get()

    val pagination: GEApiRequestPagination =
      if (update) {
        if (hasNext || !supportsTimestampUpdates) return null
        GEApiRequestPagination(iterationData.timestamp, pageSize)
      }
      else {
        if (!hasNext) return null
        GEApiRequestPagination(iterationData.cursor, pageSize)
      }

    val executionDate = Date()
    val response = executor.execute(progressIndicator, requestProducer(pagination))
    val page = extractPageInfo(response)
    iterationDataRef.compareAndSet(iterationData, IterationData(page, executionDate))

    return extractResult(response)
  }

  fun reset() {
    iterationDataRef.set(IterationData(true))
  }

  protected abstract fun extractPageInfo(result: T): GEApiCursorPageInfoDTO
  protected abstract fun extractResult(result: T): R

  private class IterationData(val hasNext: Boolean, val timestamp: Date? = null, val cursor: Int? = 1) {
    constructor(page: GEApiCursorPageInfoDTO, timestamp: Date) : this(page.hasNextPage, timestamp, page.endCursor)
  }
}