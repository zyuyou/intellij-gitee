// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data

import com.gitee.api.util.SimpleGEGQLPagesLoader
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager

open class GEGQLPagedListLoader<T>(progressManager: ProgressManager,
                                   private val loader: SimpleGEGQLPagesLoader<T>
)
  : GEListLoaderBase<T>(progressManager) {

  override fun canLoadMore() = !loading && (loader.hasNext || error != null)

  override fun doLoadMore(indicator: ProgressIndicator, update: Boolean) = loader.loadNext(indicator, update)

  override fun reset() {
    loader.reset()
    super.reset()
  }
}
