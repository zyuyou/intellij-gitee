// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api

data class GiteeRepositoryPath(private val serverPath: GiteeServerPath,
                               private val repositoryPath: com.gitee.api.GiteeFullPath) {
  fun toUrl(): String {
    return serverPath.toUrl() + "/" + repositoryPath.fullName
  }

  override fun toString(): String {
    return "$serverPath/${repositoryPath.fullName}"
  }
}
