// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data.service

import com.gitee.api.data.GERepositoryPermissionLevel
import com.gitee.api.data.GiteeUser
import com.gitee.authentication.accounts.GiteeAccount


interface GEPRSecurityService {
  val account: GiteeAccount
  val currentUser: GiteeUser

  fun isCurrentUser(user: GiteeUser): Boolean

  fun currentUserHasPermissionLevel(level: GERepositoryPermissionLevel): Boolean

  fun isMergeAllowed(): Boolean
  fun isRebaseMergeAllowed(): Boolean
  fun isSquashMergeAllowed(): Boolean

  fun isMergeForbiddenForProject(): Boolean
  fun isUserInAnyTeam(slugs: List<String>): Boolean
}