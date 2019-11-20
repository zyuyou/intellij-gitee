// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data.service

import com.gitee.api.data.GiteeAuthenticatedUser
import com.gitee.api.data.GiteeRepoDetailed
import com.gitee.api.data.GiteeUser
import com.gitee.util.GiteeSharedProjectSettings

class GiteePullRequestsSecurityServiceImpl(private val sharedProjectSettings: GiteeSharedProjectSettings,
                                           private val currentUser: GiteeAuthenticatedUser,
                                           private val repo: GiteeRepoDetailed) : GiteePullRequestsSecurityService {
  override fun isCurrentUser(user: GiteeUser) = user == currentUser
  override fun isCurrentUserWithPushAccess() = repo.permissions.isPush || repo.permissions.isAdmin

  override fun isMergeAllowed() = repo.allowMergeCommit
  override fun isRebaseMergeAllowed() = repo.allowRebaseMerge
  override fun isSquashMergeAllowed() = repo.allowSquashMerge

  override fun isMergeForbiddenForProject() = sharedProjectSettings.pullRequestMergeForbidden
}