// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.util

import com.gitee.api.GERepositoryCoordinates
import com.gitee.api.GiteeServerPath
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import git4idea.ui.branch.GitRepositoryMappingData

class GEGitRepositoryMapping(val geRepositoryCoordinates: GERepositoryCoordinates, val gitRemoteUrlCoordinates: GitRemoteUrlCoordinates) : GitRepositoryMappingData {
  override val gitRemote: GitRemote
    get() = gitRemoteUrlCoordinates.remote
  override val gitRepository: GitRepository
    get() = gitRemoteUrlCoordinates.repository
  override val repositoryPath: String
    get() = geRepositoryCoordinates.repositoryPath.repository

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is GEGitRepositoryMapping) return false

    if (geRepositoryCoordinates != other.geRepositoryCoordinates) return false

    return true
  }

  override fun hashCode(): Int {
    return geRepositoryCoordinates.hashCode()
  }

  override fun toString(): String {
    return "(repository=$geRepositoryCoordinates, remote=$gitRemoteUrlCoordinates)"
  }

  companion object {
    fun create(server: GiteeServerPath, remote: GitRemoteUrlCoordinates): GEGitRepositoryMapping? {
      val repositoryPath = GiteeUrlUtil.getUserAndRepositoryFromRemoteUrl(remote.url) ?: return null
      val repository = GERepositoryCoordinates(server, repositoryPath)
      return GEGitRepositoryMapping(repository, remote)
    }
  }
}