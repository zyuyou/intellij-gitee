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

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequests
import com.gitee.api.data.GiteeUser
import com.google.common.cache.CacheBuilder
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.util.LowMemoryWatcher
import com.intellij.util.ImageLoader
import com.intellij.util.concurrency.AppExecutorUtil
import java.awt.Image
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.function.Supplier

class CachingGiteeUserAvatarLoader(private val progressManager: ProgressManager) : Disposable {
  private val LOG = logger<CachingGiteeUserAvatarLoader>()

  private val executor = AppExecutorUtil.getAppExecutorService()
  private val progressIndicator: EmptyProgressIndicator = NonReusableEmptyProgressIndicator()

  private val avatarCache = CacheBuilder.newBuilder()
    .expireAfterAccess(5, TimeUnit.MINUTES)
    .build<GiteeUser, CompletableFuture<Image?>>()

  init {
    LowMemoryWatcher.register(Runnable { avatarCache.invalidateAll() }, this)
  }

  fun requestAvatar(requestExecutor: GiteeApiRequestExecutor, user: GiteeUser): CompletableFuture<Image?> {
    val indicator = progressIndicator
    // store images at maximum used size with maximum reasonable scale to avoid upscaling (3 for system scale, 2 for user scale)
    val imageSize = MAXIMUM_ICON_SIZE * 6

    return avatarCache.get(user) {
      val url = user.avatarUrl
      if (url == null) CompletableFuture.completedFuture(null)
      else CompletableFuture.supplyAsync(Supplier {
        try {
          progressManager.runProcess(Computable { loadAndDownscale(requestExecutor, indicator, url, imageSize) }, indicator)
        }
        catch (e: ProcessCanceledException) {
          null
        }
      }, executor)
    }
  }

  private fun loadAndDownscale(requestExecutor: GiteeApiRequestExecutor, indicator: EmptyProgressIndicator,
                               url: String, maximumSize: Int): Image? {
    try {
      val image = requestExecutor.execute(indicator, GiteeApiRequests.CurrentUser.getAvatar(url))
      return if (image.getWidth(null) <= maximumSize && image.getHeight(null) <= maximumSize) image
      else ImageLoader.scaleImage(image, maximumSize)
    }
    catch (e: ProcessCanceledException) {
      return null
    }
    catch (e: Exception) {
      LOG.info("Error loading image from $url", e)
      return null
    }
  }

  override fun dispose() {
    progressIndicator.cancel()
  }

  companion object {
    private const val MAXIMUM_ICON_SIZE = 40
  }
}