// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data.provider

import com.gitee.api.data.pullrequest.GEPullRequestFileViewedState
import com.intellij.openapi.Disposable
import com.intellij.util.concurrency.annotations.RequiresEdt
import java.util.concurrent.CompletableFuture

interface GEPRViewedStateDataProvider {

  @RequiresEdt
  fun loadViewedState(): CompletableFuture<Map<String, GEPullRequestFileViewedState>>

  @RequiresEdt
  fun getViewedState(): Map<String, GEPullRequestFileViewedState>

  @RequiresEdt
  fun updateViewedState(path: String, isViewed: Boolean)

  @RequiresEdt
  fun addViewedStateListener(parent: Disposable, listener: () -> Unit)

  @RequiresEdt
  fun reset()
}