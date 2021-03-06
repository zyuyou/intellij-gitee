// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data

import com.gitee.pullrequest.ui.SimpleEventListener
import com.gitee.util.GiteeAsyncUtil
import com.gitee.util.NonReusableEmptyProgressIndicator
import com.gitee.util.handleOnEdt
import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Computable
import com.intellij.util.EventDispatcher
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import kotlin.properties.Delegates

abstract class GiteeListLoaderBase<T>(protected val progressManager: ProgressManager)
  : GiteeListLoader, Disposable {

  private var lastFuture = CompletableFuture.completedFuture(emptyList<T>())
  private var progressIndicator = NonReusableEmptyProgressIndicator()

  private val loadingStateChangeEventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)
  override var loading: Boolean by Delegates.observable(false) { _, _, _ ->
    loadingStateChangeEventDispatcher.multicaster.eventOccurred()
  }

  private val errorChangeEventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)
  override var error: Throwable? by Delegates.observable<Throwable?>(null) { _, _, _ ->
    errorChangeEventDispatcher.multicaster.eventOccurred()
  }

  override fun canLoadMore() = !loading && (error != null)

  override fun loadMore() {
    val indicator = progressIndicator
    if (canLoadMore()) {
      loading = true
      requestLoadMore(indicator).handleOnEdt { list, error ->
        if (indicator.isCanceled) return@handleOnEdt
        when {
          error != null && !GiteeAsyncUtil.isCancellation(error) -> {
            loading = false
            this.error = if (error is CompletionException) error.cause!! else error
          }
          list != null -> {
            loading = false
            handleResult(list)
          }
        }
      }
    }
  }

  abstract fun handleResult(list: List<T>)

  private fun requestLoadMore(indicator: ProgressIndicator): CompletableFuture<List<T>> {
    lastFuture = lastFuture.thenApplyAsync {
      progressManager.runProcess(Computable { doLoadMore(indicator) }, indicator)
    }
    return lastFuture
  }

  protected abstract fun doLoadMore(indicator: ProgressIndicator): List<T>?

  override fun reset() {
    lastFuture = lastFuture.handle { _, _ ->
      listOf<T>()
    }

    progressIndicator.cancel()
    progressIndicator = NonReusableEmptyProgressIndicator()
    error = null
    loading = false
  }

  override fun addLoadingStateChangeListener(disposable: Disposable, listener: () -> Unit) =
    SimpleEventListener.addDisposableListener(loadingStateChangeEventDispatcher, disposable, listener)

  override fun addErrorChangeListener(disposable: Disposable, listener: () -> Unit) =
    SimpleEventListener.addDisposableListener(errorChangeEventDispatcher, disposable, listener)

  override fun dispose() = progressIndicator.cancel()
}