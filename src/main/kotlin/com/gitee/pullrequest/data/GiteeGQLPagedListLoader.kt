// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data

import com.gitee.api.util.SimpleGiteeGQLPagesLoader
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager

abstract class GiteeGQLPagedListLoader<T>(progressManager: ProgressManager,
                                          private val loader: SimpleGiteeGQLPagesLoader<T>)
  : GiteeListLoaderBase<T>(progressManager) {

  override fun canLoadMore() = !loading && (loader.hasNext || error != null)

  override fun doLoadMore(indicator: ProgressIndicator) = loader.loadNext(indicator)

  override fun reset() {
    loader.reset()
    super.reset()
  }
}
