// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data.provider

import com.gitee.api.data.pullrequest.GEPullRequestFileViewedState
import com.gitee.pullrequest.data.GEPRIdentifier
import com.gitee.pullrequest.data.service.GEPRFilesService
import com.gitee.util.LazyCancellableBackgroundProcessValue
import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.EmptyProgressIndicator
import java.util.concurrent.CompletableFuture

internal class GEPRViewedStateDataProviderImpl(
  private val filesService: GEPRFilesService,
  private val pullRequestId: GEPRIdentifier,
) : GEPRViewedStateDataProvider,
    Disposable {

  private val viewedState = LazyCancellableBackgroundProcessValue.create { indicator ->
    filesService
      .loadFiles(indicator, pullRequestId)
      .thenApply { files -> files.associateBy({ it.path }, { it.viewerViewedState }) }
  }

  override fun loadViewedState(): CompletableFuture<Map<String, GEPullRequestFileViewedState>> = viewedState.value

  override fun getViewedState(): Map<String, GEPullRequestFileViewedState> {
    if (!viewedState.isCached) return emptyMap()

    return runCatching { viewedState.value.getNow(emptyMap()) }.getOrDefault(emptyMap())
  }

  override fun updateViewedState(path: String, isViewed: Boolean) {
    val result = filesService.updateViewedState(EmptyProgressIndicator(), pullRequestId, path, isViewed)

    viewedState.combineResult(result) { files, _ ->
      val newState = if (isViewed) GEPullRequestFileViewedState.VIEWED else GEPullRequestFileViewedState.UNVIEWED

      files + (path to newState)
    }
  }

  override fun addViewedStateListener(parent: Disposable, listener: () -> Unit) =
    viewedState.addDropEventListener(parent, listener)

  override fun reset() = viewedState.drop()

  override fun dispose() = reset()
}