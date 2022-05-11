package com.gitee.api.util

import com.gitee.pullrequest.data.GEListLoaderBase
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager

open class GEApiPagedListLoader<T>(progressManager: ProgressManager,
                                   private val loader: SimpleGEApiPagesLoader<T>): GEListLoaderBase<T>(progressManager) {

  override fun canLoadMore() = !loading && (loader.hasNext || error != null)

  override fun doLoadMore(indicator: ProgressIndicator, update: Boolean) = loader.loadNext(indicator, update)

  override fun reset() {
    loader.reset()
    super.reset()
  }


}