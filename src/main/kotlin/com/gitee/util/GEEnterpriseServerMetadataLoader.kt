// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.util

import com.gitee.api.GiteeApiRequest
import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeServerPath
import com.gitee.api.data.GEEnterpriseServerMeta
import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.collaboration.util.ProgressIndicatorsProvider
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Disposer
import org.jetbrains.annotations.CalledInAny
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

@Service
class GEEnterpriseServerMetadataLoader : Disposable {

  private val apiRequestExecutor = GiteeApiRequestExecutor.Factory.getInstance().create()
  private val serverMetadataRequests = ConcurrentHashMap<GiteeServerPath, CompletableFuture<GEEnterpriseServerMeta>>()
  private val indicatorProvider = ProgressIndicatorsProvider().also {
    Disposer.register(this, it)
  }

  @CalledInAny
  fun loadMetadata(server: GiteeServerPath): CompletableFuture<GEEnterpriseServerMeta> {
    require(!server.isGiteeDotCom()) { "Cannot retrieve server metadata from github.com" }
    return serverMetadataRequests.getOrPut(server) {
      ProgressManager.getInstance().submitIOTask(indicatorProvider) {
        val metaUrl = server.toApiUrl() + "/meta"
        apiRequestExecutor.execute(it, GiteeApiRequest.Get.json<GEEnterpriseServerMeta>(metaUrl))
      }
    }
  }

  override fun dispose() {}
}