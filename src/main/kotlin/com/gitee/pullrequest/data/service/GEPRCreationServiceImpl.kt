// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data.service

import com.gitee.api.GEGQLRequests
import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.data.pullrequest.GEPullRequestShort
import com.gitee.i18n.GiteeBundle
import com.gitee.pullrequest.data.GEPRIdentifier
import com.gitee.util.GEGitRepositoryMapping
import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.text.nullize
import git4idea.GitRemoteBranch
import java.util.concurrent.CompletableFuture

class GEPRCreationServiceImpl(private val progressManager: ProgressManager,
                              private val requestExecutor: GiteeApiRequestExecutor,
                              repositoryDataService: GEPRRepositoryDataService) : GEPRCreationService {

  private val baseRepo = repositoryDataService.repositoryMapping
  private val repositoryId = repositoryDataService.repositoryId

  override fun createPullRequest(progressIndicator: ProgressIndicator,
                                 baseBranch: GitRemoteBranch,
                                 headRepo: GEGitRepositoryMapping,
                                 headBranch: GitRemoteBranch,
                                 title: String,
                                 description: String,
                                 draft: Boolean): CompletableFuture<GEPullRequestShort> =
    progressManager.submitIOTask(progressIndicator) {
      it.text = GiteeBundle.message("pull.request.create.process.title")

      val headRepositoryPrefix = getHeadRepoPrefix(headRepo)

      val actualTitle = title.takeIf(String::isNotBlank) ?: headBranch.nameForRemoteOperations
      val body = description.nullize(true)

      requestExecutor.execute(it, GEGQLRequests.PullRequest.create(baseRepo.geRepositoryCoordinates, repositoryId,
                                                                   baseBranch.nameForRemoteOperations,
                                                                   headRepositoryPrefix + headBranch.nameForRemoteOperations,
                                                                   actualTitle, body, draft
      ))
    }

  override fun findPullRequest(progressIndicator: ProgressIndicator,
                               baseBranch: GitRemoteBranch,
                               headRepo: GEGitRepositoryMapping,
                               headBranch: GitRemoteBranch): GEPRIdentifier? {
    progressIndicator.text = GiteeBundle.message("pull.request.existing.process.title")
    return requestExecutor.execute(progressIndicator,
                                   GEGQLRequests.PullRequest.findByBranches(baseRepo.geRepositoryCoordinates,
                                                                            baseBranch.nameForRemoteOperations,
                                                                            headBranch.nameForRemoteOperations
                                   )).nodes.firstOrNull {
      it.headRepository?.owner?.login == headRepo.geRepositoryCoordinates.repositoryPath.owner
    }
  }

  private fun getHeadRepoPrefix(headRepo: GEGitRepositoryMapping) =
    if (baseRepo.geRepositoryCoordinates == headRepo.geRepositoryCoordinates) "" else headRepo.geRepositoryCoordinates.repositoryPath.owner + ":"
}