// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data.service

import com.gitee.api.GERepositoryCoordinates
import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequests
import com.gitee.api.data.GiteeIssueLabel
import com.gitee.api.data.GiteeUser
import com.gitee.api.data.GiteeUserWithPermissions
import com.gitee.api.data.pullrequest.GEPullRequestRequestedReviewer
import com.gitee.api.data.pullrequest.GETeam
import com.gitee.api.util.GiteeApiPagesLoader
import com.gitee.util.GitRemoteUrlCoordinates
import com.gitee.util.LazyCancellableBackgroundProcessValue
import com.intellij.openapi.progress.ProgressManager
import java.util.concurrent.CompletableFuture
import java.util.function.BiFunction

class GEPRRepositoryDataServiceImpl internal constructor(progressManager: ProgressManager,
                                                         private val requestExecutor: GiteeApiRequestExecutor,
                                                         override val remoteCoordinates: GitRemoteUrlCoordinates,
                                                         override val repositoryCoordinates: GERepositoryCoordinates,
                                                         private val repoOwner: GiteeUser,
                                                         override val repositoryId: String,
                                                         override val defaultBranchName: String?,
                                                         override val isFork: Boolean)
  : GEPRRepositoryDataService {

  private val serverPath = repositoryCoordinates.serverPath
  private val repoPath = repositoryCoordinates.repositoryPath

  init {
    requestExecutor.addListener(this) {
      resetData()
    }
  }

  private val collaboratorsValue = LazyCancellableBackgroundProcessValue.create(progressManager) { indicator ->
    GiteeApiPagesLoader
      .loadAll(requestExecutor, indicator,
               GiteeApiRequests.Repos.Collaborators.pages(serverPath, repoPath.owner, repoPath.repository))
  }

  override val collaborators: CompletableFuture<List<GiteeUserWithPermissions>>
//    get() = collaboratorsValue.value.thenApply { list ->
//      list.map { GiteeUserWithPermissions(it.nodeId, it.login, it.htmlUrl, it.avatarUrl ?: "", null) }
//    }
    get() = collaboratorsValue.value

  private val teamsValue = LazyCancellableBackgroundProcessValue.create(progressManager) { indicator ->
//    if (repoOwner !is GERepositoryOwnerName.Organization)
//      emptyList()
//    else
//      SimpleGEGQLPagesLoader(requestExecutor, {
//        GEGQLRequests.Organization.Team.findAll(serverPath, repoOwner.login, it)
//      }).loadAll(indicator)
    emptyList<GETeam>()
  }

  override val teams: CompletableFuture<List<GETeam>>
    get() = teamsValue.value

  override val potentialReviewers: CompletableFuture<List<GEPullRequestRequestedReviewer>>
    get() = collaboratorsValue.value.thenCombine(teams,
                                                 BiFunction<List<GiteeUserWithPermissions>, List<GETeam>, List<GEPullRequestRequestedReviewer>> { users, teams ->
                                                   users
                                                     .filter { it.permissions.isPush }
//                                                     .map { GiteeUser(it.nodeId, it.login, it.htmlUrl, it.avatarUrl ?: "", null) } +
                                                   teams
                                                 })

  private val assigneesValue = LazyCancellableBackgroundProcessValue.create(progressManager) { indicator ->
    GiteeApiPagesLoader
      .loadAll(requestExecutor, indicator,
               GiteeApiRequests.Repos.Assignees.pages(serverPath, repoPath.owner, repoPath.repository))
//      .map { GiteeUser(it.nodeId, it.login, it.htmlUrl, it.avatarUrl ?: "", null) }
  }

  override val issuesAssignees: CompletableFuture<List<GiteeUser>>
    get() = assigneesValue.value

  private val labelsValue = LazyCancellableBackgroundProcessValue.create(progressManager) { indicator ->
    GiteeApiPagesLoader
      .loadAll(requestExecutor, indicator,
               GiteeApiRequests.Repos.Labels.pages(serverPath, repoPath.owner, repoPath.repository))
//      .map { GiteeIssueLabel(it.nodeId, it.url, it.name, it.color) }
  }

  override val labels: CompletableFuture<List<GiteeIssueLabel>>
    get() = labelsValue.value

  override fun resetData() {
    collaboratorsValue.drop()
    teamsValue.drop()
    assigneesValue.drop()
    labelsValue.drop()
  }

  override fun dispose() {
    resetData()
  }
}