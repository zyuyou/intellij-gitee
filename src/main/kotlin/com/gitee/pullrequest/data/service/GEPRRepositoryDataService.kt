// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data.service

import com.gitee.api.GERepositoryCoordinates
import com.gitee.api.data.GiteeIssueLabel
import com.gitee.api.data.GiteeUser
import com.gitee.api.data.GiteeUserWithPermissions
import com.gitee.api.data.pullrequest.GEPullRequestRequestedReviewer
import com.gitee.api.data.pullrequest.GETeam
import com.gitee.util.GEGitRepositoryMapping
import com.gitee.util.GitRemoteUrlCoordinates
import com.intellij.openapi.Disposable
import com.intellij.util.concurrency.annotations.RequiresEdt
import java.util.concurrent.CompletableFuture

interface GEPRRepositoryDataService : Disposable {
  val remoteCoordinates: GitRemoteUrlCoordinates
  val repositoryCoordinates: GERepositoryCoordinates
  val repositoryMapping: GEGitRepositoryMapping
    get() = GEGitRepositoryMapping(repositoryCoordinates, remoteCoordinates)

  val repositoryId: String
  val defaultBranchName: String?
  val isFork: Boolean

  val collaborators: CompletableFuture<List<GiteeUserWithPermissions>>
  val teams: CompletableFuture<List<GETeam>>
  val potentialReviewers: CompletableFuture<List<GEPullRequestRequestedReviewer>>
  val issuesAssignees: CompletableFuture<List<GiteeUser>>
  val labels: CompletableFuture<List<GiteeIssueLabel>>

  @RequiresEdt
  fun resetData()
}