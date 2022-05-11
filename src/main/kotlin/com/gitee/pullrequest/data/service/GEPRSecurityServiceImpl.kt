// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data.service

import com.gitee.api.data.GERepositoryPermissionLevel
import com.gitee.api.data.GiteeRepoDetailed
import com.gitee.api.data.GiteeUser
import com.gitee.api.data.pullrequest.GETeam
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.util.GiteeSharedProjectSettings

class GEPRSecurityServiceImpl(private val sharedProjectSettings: GiteeSharedProjectSettings,
                              override val account: GiteeAccount,
                              override val currentUser: GiteeUser,
                              private val currentUserTeams: List<GETeam>,
                              private val repo: GiteeRepoDetailed
) : GEPRSecurityService {
  override fun isCurrentUser(user: GiteeUser) = user.id == currentUser.id

//  override fun currentUserHasPermissionLevel(level: GERepositoryPermissionLevel) =
//    (repo.viewerPermission?.ordinal ?: -1) >= level.ordinal
  override fun currentUserHasPermissionLevel(level: GERepositoryPermissionLevel) =
    repo.permission.isAdmin

  override fun isUserInAnyTeam(slugs: List<String>) = currentUserTeams.any { slugs.contains(it.slug) }

//  override fun isMergeAllowed() = repo.mergeCommitAllowed
  override fun isMergeAllowed() = repo.allowMergeCommit
//  override fun isRebaseMergeAllowed() = repo.rebaseMergeAllowed
  override fun isRebaseMergeAllowed() = repo.allowRebaseMerge
//  override fun isSquashMergeAllowed() = repo.squashMergeAllowed
  override fun isSquashMergeAllowed() = repo.allowSquashMerge

  override fun isMergeForbiddenForProject() = sharedProjectSettings.pullRequestMergeForbidden
}