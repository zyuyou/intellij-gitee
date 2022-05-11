// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data.service

import com.gitee.api.GEGQLRequests
import com.gitee.api.GERepositoryCoordinates
import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.data.pullrequest.GEPullRequestChangedFile
import com.gitee.api.util.SimpleGEGQLPagesLoader
import com.gitee.pullrequest.data.GEPRIdentifier
import com.gitee.pullrequest.data.service.GEServiceUtil.logError
import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import java.util.concurrent.CompletableFuture

private val LOG = logger<GEPRFilesServiceImpl>()

class GEPRFilesServiceImpl(
  private val progressManager: ProgressManager,
  private val requestExecutor: GiteeApiRequestExecutor,
  private val repository: GERepositoryCoordinates
) : GEPRFilesService {

  override fun loadFiles(
    progressIndicator: ProgressIndicator,
    pullRequestId: GEPRIdentifier
  ): CompletableFuture<List<GEPullRequestChangedFile>> =
    progressManager
      .submitIOTask(progressIndicator) { indicator ->
        val loader = SimpleGEGQLPagesLoader(
          requestExecutor,
          { GEGQLRequests.PullRequest.files(repository, pullRequestId.number, it) }
        )

        loader.loadAll(indicator)
      }
      .logError(LOG, "Error occurred while loading pull request files")

  override fun updateViewedState(
    progressIndicator: ProgressIndicator,
    pullRequestId: GEPRIdentifier,
    path: String,
    isViewed: Boolean
  ): CompletableFuture<Unit> =
    progressManager
      .submitIOTask(progressIndicator) { indicator ->
        val request =
          if (isViewed)
            GEGQLRequests.PullRequest.markFileAsViewed(repository.serverPath, pullRequestId.id, path)
          else
            GEGQLRequests.PullRequest.unmarkFileAsViewed(repository.serverPath, pullRequestId.id, path)

        requestExecutor.execute(indicator, request)
      }
      .logError(LOG, "Error occurred while updating file viewed state")
}