// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data

import com.gitee.api.GiteeRepositoryPath

class GERepositoryPermission(id: String,
                             nameWithOwner: String,
                             val viewerPermission: GiteeRepositoryPermissionLevel?,
                             val mergeCommitAllowed: Boolean,
                             val squashMergeAllowed: Boolean,
                             val rebaseMergeAllowed: Boolean)
  : GENode(id) {
  val path: GiteeRepositoryPath

  init {
    val split = nameWithOwner.split('/')
    path = GiteeRepositoryPath(split[0], split[1])
  }
}