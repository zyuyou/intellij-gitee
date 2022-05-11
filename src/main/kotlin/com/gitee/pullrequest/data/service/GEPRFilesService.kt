// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data.service

import com.gitee.api.data.pullrequest.GEPullRequestChangedFile
import com.gitee.pullrequest.data.GEPRIdentifier
import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.annotations.CalledInAny
import java.util.concurrent.CompletableFuture

interface GEPRFilesService {

  @CalledInAny
  fun loadFiles(
    progressIndicator: ProgressIndicator,
    pullRequestId: GEPRIdentifier
  ): CompletableFuture<List<GEPullRequestChangedFile>>

  @CalledInAny
  fun updateViewedState(
    progressIndicator: ProgressIndicator,
    pullRequestId: GEPRIdentifier,
    path: String,
    isViewed: Boolean
  ): CompletableFuture<Unit>
}