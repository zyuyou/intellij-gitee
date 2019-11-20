// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui

import com.gitee.util.GiteeAsyncUtil
import com.gitee.util.handleOnEdt
import com.intellij.util.EventDispatcher
import org.jetbrains.annotations.CalledInAwt
import java.util.concurrent.CompletableFuture
import kotlin.properties.Delegates.observable

class GiteeCompletableFutureLoadingModel<T> : GiteeLoadingModel<T> {
  override var loading: Boolean = false

  override var result: T? = null
  override var error: Throwable? = null

  private val eventDispatcher = EventDispatcher.create(GiteeLoadingModel.StateChangeListener::class.java)
  //to cancel old callbacks
  private var updateFuture by observable<CompletableFuture<Unit>?>(null) { _, oldValue, _ ->
    oldValue?.cancel(true)
  }

  @set:CalledInAwt
  @get:CalledInAwt
  var future by observable<CompletableFuture<T>?>(null) { _, _, newValue ->
    if (newValue != null) load(newValue) else reset()
  }

  private fun load(future: CompletableFuture<T>) {
    loading = true
    eventDispatcher.multicaster.onLoadingStarted()
    updateFuture = future.let {
      it.handleOnEdt { result, error ->
        when {
          error != null && !GiteeAsyncUtil.isCancellation(error) -> this.error = error.cause
          result != null -> this.result = result
        }
        loading = false
        eventDispatcher.multicaster.onLoadingCompleted()
      }
    }
  }

  private fun reset() {
    updateFuture = null
    loading = false
    result = null
    error = null
    eventDispatcher.multicaster.onReset()
  }

  override fun addStateChangeListener(listener: GiteeLoadingModel.StateChangeListener) = eventDispatcher.addListener(listener)
  override fun removeStateChangeListener(listener: GiteeLoadingModel.StateChangeListener) = eventDispatcher.removeListener(listener)
}