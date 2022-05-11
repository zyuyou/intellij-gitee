// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data.provider

import com.gitee.api.data.GECommit
import com.gitee.pullrequest.data.GEPRChangesProvider
import com.intellij.openapi.Disposable
import com.intellij.util.concurrency.annotations.RequiresEdt
import java.util.concurrent.CompletableFuture

interface GEPRChangesDataProvider {

  @RequiresEdt
  fun loadChanges(): CompletableFuture<GEPRChangesProvider>

  @RequiresEdt
  fun reloadChanges()

  @RequiresEdt
  fun addChangesListener(disposable: Disposable, listener: () -> Unit)

  @RequiresEdt
  fun loadChanges(disposable: Disposable, consumer: (CompletableFuture<GEPRChangesProvider>) -> Unit) {
    addChangesListener(disposable) {
      consumer(loadChanges())
    }
    consumer(loadChanges())
  }

  @RequiresEdt
  fun loadCommitsFromApi(): CompletableFuture<List<GECommit>>

  @RequiresEdt
  fun addCommitsListener(disposable: Disposable, listener: () -> Unit)

  @RequiresEdt
  fun loadCommitsFromApi(disposable: Disposable, consumer: (CompletableFuture<List<GECommit>>) -> Unit) {
    addCommitsListener(disposable) {
      consumer(loadCommitsFromApi())
    }
    consumer(loadCommitsFromApi())
  }

  @RequiresEdt
  fun fetchBaseBranch(): CompletableFuture<Unit>

  @RequiresEdt
  fun fetchHeadBranch(): CompletableFuture<Unit>
}