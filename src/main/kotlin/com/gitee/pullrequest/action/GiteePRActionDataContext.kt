// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.action

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeRepositoryCoordinates
import com.gitee.pullrequest.avatars.CachingGiteeAvatarIconsProvider
import com.gitee.pullrequest.data.GiteePullRequestDataProvider
import com.gitee.pullrequest.data.GiteePullRequestsDataContext
import com.gitee.pullrequest.ui.GiteePullRequestsListSelectionHolder
import com.gitee.util.GitRemoteUrlCoordinates

class GiteePRActionDataContext internal constructor(private val dataContext: GiteePullRequestsDataContext,
                                                    private val selectionHolder: GiteePullRequestsListSelectionHolder,
                                                    val avatarIconsProviderFactory: CachingGiteeAvatarIconsProvider.Factory) {

  val gitRepositoryCoordinates: GitRemoteUrlCoordinates = dataContext.gitRepositoryCoordinates
  val repositoryCoordinates: GiteeRepositoryCoordinates = dataContext.repositoryCoordinates
  val requestExecutor: GiteeApiRequestExecutor = dataContext.requestExecutor

  fun resetAllData() {
    dataContext.metadataService.resetData()
    dataContext.listLoader.reset()
    dataContext.dataLoader.invalidateAllData()
  }

  private val selectedPullRequest: Long?
    get() = selectionHolder.selectionNumber

  val selectedPullRequestDataProvider: GiteePullRequestDataProvider?
    get() = selectedPullRequest?.let { dataContext.dataLoader.getDataProvider(it) }

}