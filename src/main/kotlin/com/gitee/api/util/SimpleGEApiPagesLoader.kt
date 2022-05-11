package com.gitee.api.util

import com.gitee.api.GiteeApiRequest
import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.data.GEApiPagedResponseDataDTO
import com.gitee.api.data.request.GEApiRequestPagination
import com.gitee.api.data.request.GiteeRequestPagination
import com.intellij.openapi.progress.ProgressIndicator

class SimpleGEApiPagesLoader<T>(executor: GiteeApiRequestExecutor,
                                requestProducer: (GEApiRequestPagination) -> GiteeApiRequest.Get<GEApiPagedResponseDataDTO<T>>,
                                supportsTimestampUpdates: Boolean = false,
                                pageSize: Int = GiteeRequestPagination.DEFAULT_PAGE_SIZE)
  : GEApiPagesLoader<GEApiPagedResponseDataDTO<T>, List<T>>(executor, requestProducer, supportsTimestampUpdates, pageSize) {

  fun loadAll(progressIndicator: ProgressIndicator): List<T> {
    val list = mutableListOf<T>()
    while (hasNext) {
      loadNext(progressIndicator)?.let { list.addAll(it) }
    }
    return list
  }

  override fun extractPageInfo(result: GEApiPagedResponseDataDTO<T>) = result.pageInfo

  override fun extractResult(result: GEApiPagedResponseDataDTO<T>) = result.items
}