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

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Computable
import com.intellij.ui.scale.ScaleContext
import com.intellij.util.ImageLoader
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.ImageUtil
import java.awt.Image
import java.util.concurrent.CompletableFuture
import java.util.function.Supplier

class GiteeImageResizer : Disposable {

  private val executor = AppExecutorUtil.createBoundedApplicationPoolExecutor("Gitee Image Resizer", getThreadPoolSize())
  private val progressIndicator: EmptyProgressIndicator = NonReusableEmptyProgressIndicator()

  fun requestImageResize(image: Image, size: Int, scaleContext: ScaleContext): CompletableFuture<Image> {
    val indicator = progressIndicator

    return CompletableFuture.supplyAsync(Supplier {
      ProgressManager.getInstance().runProcess(Computable {
        indicator.checkCanceled()
        val hidpiImage = ImageUtil.ensureHiDPI(image, scaleContext)
        indicator.checkCanceled()
        ImageLoader.scaleImage(hidpiImage, size)
      }, indicator)
    }, executor)
  }

  override fun dispose() {
    progressIndicator.cancel()
    executor.shutdownNow()
  }

  companion object {
    private fun getThreadPoolSize() = Math.max(Runtime.getRuntime().availableProcessors() / 2, 1)

    @JvmStatic
    fun getInstance(): GiteeImageResizer = service()
  }
}
