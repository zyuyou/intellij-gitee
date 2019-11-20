// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data.service

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequests
import com.gitee.api.GiteeRepositoryPath
import com.gitee.api.GiteeServerPath
import com.gitee.api.data.GELabel
import com.gitee.api.data.GEUser
import com.gitee.api.util.GiteeApiPagesLoader
import com.gitee.pullrequest.data.GiteePullRequestsDataContext.Companion.PULL_REQUEST_EDITED_TOPIC
import com.gitee.util.CollectionDelta
import com.gitee.util.GiteeAsyncUtil
import com.gitee.util.LazyCancellableBackgroundProcessValue
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.messages.MessageBus
import org.jetbrains.annotations.CalledInBackground
import java.util.concurrent.CompletableFuture

class GiteePullRequestsMetadataServiceImpl internal constructor(progressManager: ProgressManager,
                                                                private val messageBus: MessageBus,
                                                                private val requestExecutor: GiteeApiRequestExecutor,
                                                                private val serverPath: GiteeServerPath,
                                                                private val repoPath: GiteeRepositoryPath)
  : GiteePullRequestsMetadataService {

  init {
    requestExecutor.addListener(this) {
      resetData()
    }
  }

  private val collaboratorsValue = LazyCancellableBackgroundProcessValue.create(progressManager) { indicator ->
    GiteeApiPagesLoader
      .loadAll(requestExecutor, indicator,
               GiteeApiRequests.Repos.Collaborators.pages(serverPath, repoPath.owner, repoPath.repository))
      .filter { it.permissions.isPush }
      .map { GEUser(it.nodeId, it.login, it.htmlUrl, it.avatarUrl ?: "", null) }
  }

  override val collaboratorsWithPushAccess: CompletableFuture<List<GEUser>>
    get() = GiteeAsyncUtil.futureOfMutable { invokeAndWaitIfNeeded { collaboratorsValue.value } }

  private val assigneesValue = LazyCancellableBackgroundProcessValue.create(progressManager) { indicator ->
    GiteeApiPagesLoader
      .loadAll(requestExecutor, indicator,
               GiteeApiRequests.Repos.Assignees.pages(serverPath, repoPath.owner, repoPath.repository))
      .map { GEUser(it.nodeId, it.login, it.htmlUrl, it.avatarUrl ?: "", null) }
  }

  override val issuesAssignees: CompletableFuture<List<GEUser>>
    get() = GiteeAsyncUtil.futureOfMutable { invokeAndWaitIfNeeded { assigneesValue.value } }
  private val labelsValue = LazyCancellableBackgroundProcessValue.create(progressManager) { indicator ->
    GiteeApiPagesLoader
      .loadAll(requestExecutor, indicator,
               GiteeApiRequests.Repos.Labels.pages(serverPath, repoPath.owner, repoPath.repository))
      .map { GELabel(it.nodeId, it.url, it.name, it.color) }
  }

  override val labels: CompletableFuture<List<GELabel>>
    get() = GiteeAsyncUtil.futureOfMutable { invokeAndWaitIfNeeded { labelsValue.value } }

  override fun resetData() {
    collaboratorsValue.drop()
    assigneesValue.drop()
    labelsValue.drop()
  }

  @CalledInBackground
  override fun adjustReviewers(indicator: ProgressIndicator, pullRequest: Long, delta: CollectionDelta<GEUser>) {
    if (delta.isEmpty) return

    if (delta.removedItems.isNotEmpty()) {
      indicator.text2 = "Removing reviewers"
      requestExecutor.execute(indicator,
                              GiteeApiRequests.Repos.PullRequests.Reviewers
                                .remove(serverPath, repoPath.owner, repoPath.repository, pullRequest,
                                        delta.removedItems.map { it.login }))
    }
    if (delta.newItems.isNotEmpty()) {
      indicator.text2 = "Adding reviewers"
      requestExecutor.execute(indicator,
                              GiteeApiRequests.Repos.PullRequests.Reviewers
                                .add(serverPath, repoPath.owner, repoPath.repository, pullRequest,
                                     delta.newItems.map { it.login }))
    }
    messageBus.syncPublisher(PULL_REQUEST_EDITED_TOPIC).onPullRequestEdited(pullRequest)
  }

  @CalledInBackground
  override fun adjustAssignees(indicator: ProgressIndicator, pullRequest: Long, delta: CollectionDelta<GEUser>) {
    if (delta.isEmpty) return

    requestExecutor.execute(indicator,
                            GiteeApiRequests.Repos.Issues.updateAssignees(serverPath, repoPath.owner, repoPath.repository,
                                                                           pullRequest.toString(), delta.newCollection.map { it.login }))
    messageBus.syncPublisher(PULL_REQUEST_EDITED_TOPIC).onPullRequestEdited(pullRequest)
  }

  @CalledInBackground
  override fun adjustLabels(indicator: ProgressIndicator, pullRequest: Long, delta: CollectionDelta<GELabel>) {
    if (delta.isEmpty) return

    requestExecutor.execute(indicator,
                            GiteeApiRequests.Repos.Issues.Labels
                              .replace(serverPath, repoPath.owner, repoPath.repository, pullRequest.toString(),
                                       delta.newCollection.map { it.name }))
    messageBus.syncPublisher(PULL_REQUEST_EDITED_TOPIC).onPullRequestEdited(pullRequest)
  }

  override fun dispose() {
    resetData()
  }
}