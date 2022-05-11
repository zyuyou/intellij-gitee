// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.details

import com.gitee.api.data.GERepositoryPermissionLevel
import com.gitee.api.data.GiteeIssueLabel
import com.gitee.api.data.GiteePullRequestDetailed
import com.gitee.api.data.GiteeUser
import com.gitee.api.data.pullrequest.GEPullRequestRequestedReviewer
import com.gitee.pullrequest.data.provider.GEPRDetailsDataProvider
import com.gitee.pullrequest.data.service.GEPRRepositoryDataService
import com.gitee.pullrequest.data.service.GEPRSecurityService
import com.gitee.util.CollectionDelta
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.openapi.progress.ProgressIndicator

class GEPRMetadataModelImpl(private val valueModel: SingleValueModel<GiteePullRequestDetailed>,
                            securityService: GEPRSecurityService,
                            repositoryDataService: GEPRRepositoryDataService,
                            private val detailsDataProvider: GEPRDetailsDataProvider
) : GEPRMetadataModelBase(repositoryDataService) {

  override val assignees: List<GiteeUser>
    get() = valueModel.value.assignees
  override val reviewers: List<GEPullRequestRequestedReviewer>
//    get() = valueModel.value.reviewRequests.mapNotNull { it.requestedReviewer }
    get() = valueModel.value.reviewers
  override val labels: List<GiteeIssueLabel>
    get() = valueModel.value.labels

  override fun getAuthor() = valueModel.value.author as? GiteeUser

  override val isEditingAllowed = securityService.currentUserHasPermissionLevel(GERepositoryPermissionLevel.TRIAGE)

  override fun adjustReviewers(indicator: ProgressIndicator, delta: CollectionDelta<GEPullRequestRequestedReviewer>) =
    detailsDataProvider.adjustReviewers(indicator, delta)

  override fun adjustAssignees(indicator: ProgressIndicator, delta: CollectionDelta<GiteeUser>) =
    detailsDataProvider.adjustAssignees(indicator, delta)

  override fun adjustLabels(indicator: ProgressIndicator, delta: CollectionDelta<GiteeIssueLabel>) =
    detailsDataProvider.adjustLabels(indicator, delta)

  override fun addAndInvokeChangesListener(listener: () -> Unit) =
    valueModel.addAndInvokeListener { listener() }
}