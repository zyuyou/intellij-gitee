// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.toolwindow.create

import com.gitee.api.data.GiteeIssueLabel
import com.gitee.api.data.GiteeUser
import com.gitee.api.data.pullrequest.GEPullRequestRequestedReviewer
import com.gitee.pullrequest.data.service.GEPRRepositoryDataService
import com.gitee.pullrequest.ui.details.GEPRMetadataModelBase
import com.gitee.util.CollectionDelta
import com.intellij.collaboration.ui.SimpleEventListener
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.EventDispatcher
import java.util.concurrent.CompletableFuture
import kotlin.properties.Delegates.observable

class GEPRCreateMetadataModel(repositoryDataService: GEPRRepositoryDataService,
                              private val currentUser: GiteeUser
)
  : GEPRMetadataModelBase(repositoryDataService) {

  private val eventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)

  override var assignees: List<GiteeUser> by observable(emptyList()) { _, _, _ ->
    eventDispatcher.multicaster.eventOccurred()
  }
  override var reviewers: List<GEPullRequestRequestedReviewer> by observable(emptyList()) { _, _, _ ->
    eventDispatcher.multicaster.eventOccurred()
  }
  override var labels: List<GiteeIssueLabel> by observable(emptyList()) { _, _, _ ->
    eventDispatcher.multicaster.eventOccurred()
  }

  override val isEditingAllowed = true

  override fun getAuthor() = currentUser

  override fun adjustAssignees(indicator: ProgressIndicator, delta: CollectionDelta<GiteeUser>): CompletableFuture<Unit> {
    assignees = ArrayList(delta.newCollection)
    return CompletableFuture.completedFuture(Unit)
  }

  override fun adjustReviewers(indicator: ProgressIndicator,
                               delta: CollectionDelta<GEPullRequestRequestedReviewer>): CompletableFuture<Unit> {
    reviewers = ArrayList(delta.newCollection)
    return CompletableFuture.completedFuture(Unit)
  }

  override fun adjustLabels(indicator: ProgressIndicator, delta: CollectionDelta<GiteeIssueLabel>): CompletableFuture<Unit> {
    labels = ArrayList(delta.newCollection)
    return CompletableFuture.completedFuture(Unit)
  }

  override fun addAndInvokeChangesListener(listener: () -> Unit) = SimpleEventListener.addAndInvokeListener(eventDispatcher, listener)
}