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
package com.gitee.util

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.PerformInBackgroundOption
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import java.util.concurrent.*
import java.util.function.BiFunction

object GiteeAsyncUtil {

  /**
   * Run [consumer] on EDT with the result of [future]
   * If future is cancelled, [consumer] will not be executed
   *
   * This is a naive implementation with timeout waiting
   */
  @JvmStatic
  fun <R : Future<T>, T> awaitFutureAndRunOnEdt(future: R,
                                                project: Project, title: String, errorTitle: String,
                                                consumer: (T) -> Unit) {
    object : Task.Backgroundable(project, title, true, PerformInBackgroundOption.DEAF) {
      var result: T? = null

      override fun run(indicator: ProgressIndicator) {
        while (true) {
          try {
            result = future.get(50, TimeUnit.MILLISECONDS)
            break
          }
          catch (e: TimeoutException) {
            indicator.checkCanceled()
          }
        }
        indicator.checkCanceled()
      }

      override fun onSuccess() {
        result?.let(consumer)
      }

      override fun onThrowable(error: Throwable) {
        if (isCancellation(error)) return
        GiteeNotifications.showError(project, errorTitle, error)
      }
    }.queue()
  }

  fun isCancellation(error: Throwable): Boolean {
    return error is ProcessCanceledException
           || error is CancellationException
           || error is InterruptedException
           || error.cause?.let(::isCancellation) ?: false
  }
}

fun <T> CompletableFuture<T>.handleOnEdt(handler: (T?, Throwable?) -> Unit): CompletableFuture<Unit> =
  handleAsync(BiFunction<T?, Throwable?, Unit> { result: T?, error: Throwable? ->
    handler(result, error)
  }, EDT_EXECUTOR)

val EDT_EXECUTOR = Executor { runnable -> runInEdt { runnable.run() } }