// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data

import com.gitee.api.GERepositoryPath
import com.gitee.api.data.pullrequest.GEGitRefName
import com.intellij.collaboration.api.dto.GraphQLFragment

@GraphQLFragment("/graphql/fragment/repositoryInfo.graphql")
class GERepository(id: String,
                   val owner: GERepositoryOwnerName,
                   nameWithOwner: String,
                   val viewerPermission: GERepositoryPermissionLevel?,
                   val mergeCommitAllowed: Boolean,
                   val squashMergeAllowed: Boolean,
                   val rebaseMergeAllowed: Boolean,
                   @Suppress("MemberVisibilityCanBePrivate") val defaultBranchRef: GEGitRefName?,
                   val isFork: Boolean)
  : GENode(id) {
  val path: GERepositoryPath
  val defaultBranch = defaultBranchRef?.name

  init {
    val split = nameWithOwner.split('/')
    path = GERepositoryPath(split[0], split[1])
  }
}