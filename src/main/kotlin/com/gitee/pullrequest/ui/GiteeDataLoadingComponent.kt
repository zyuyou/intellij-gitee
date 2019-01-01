/*
 *  Copyright 2016-2019 码云 - Gitee
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.gitee.pullrequest.ui

import com.gitee.util.GiteeAsyncUtil
import com.gitee.util.handleOnEdt
import com.intellij.ui.components.panels.Wrapper
import org.jetbrains.annotations.CalledInAwt
import java.util.concurrent.CompletableFuture

/**
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/pullrequest/ui/GithubDataLoadingComponent.kt
 * @author JetBrains s.r.o.
 */
internal abstract class GiteeDataLoadingComponent<T> : Wrapper() {
  private var updateFuture: CompletableFuture<Unit>? = null

  /**
   * This works because [handleOnEdt] basically forms a EDT-synchronized section and result/exception is acquired from [dataRequest] on EDT
   *
   * In pseudocode:
   * when (dataRequest.isDone) { runOnEdt { handler(getResult(), getException()) } }
   */
  @CalledInAwt
  fun loadAndShow(dataRequest: CompletableFuture<T>?) {
    updateFuture?.cancel(true)
    reset()

    if (dataRequest == null) {
      updateFuture = null
      setBusy(false)
      return
    }

    setBusy(true)
    updateFuture = dataRequest.handleOnEdt { result, error ->
      when {
        error != null && !GiteeAsyncUtil.isCancellation(error) -> handleError(error)
        result != null -> handleResult(result)
      }
      setBusy(false)
    }
  }

  protected abstract fun reset()
  protected abstract fun handleResult(result: T)
  protected abstract fun handleError(error: Throwable)
  protected abstract fun setBusy(busy: Boolean)
}