// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.util

import com.gitee.api.GERepositoryCoordinates
import com.gitee.api.GiteeServerPath
import git4idea.remote.GitRemoteUrlCoordinates
import git4idea.remote.hosting.HostedGitRepositoryMapping
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import git4idea.ui.branch.GitRepositoryMappingData

class GEGitRepositoryMapping(override val repository: GERepositoryCoordinates,
                             override val remote: GitRemoteUrlCoordinates,
) : GitRepositoryMappingData, HostedGitRepositoryMapping {

  override val gitRemote: GitRemote
    get() = remote.remote
  override val gitRepository: GitRepository
    get() = remote.repository
  override val repositoryPath: String
    get() = repository.repositoryPath.repository

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is GEGitRepositoryMapping) return false

    if (repository != other.repository) return false

    return true
  }

  override fun hashCode(): Int {
    return repository.hashCode()
  }

  override fun toString(): String {
    return "(repository=$repository, remote=$repository)"
  }

  companion object {
    fun create(server: GiteeServerPath, remote: GitRemoteUrlCoordinates): GEGitRepositoryMapping? {
      val repositoryPath = GiteeUrlUtil.getUserAndRepositoryFromRemoteUrl(remote.url) ?: return null
      val repository = GERepositoryCoordinates(server, repositoryPath)
      return GEGitRepositoryMapping(repository, remote)
    }
  }
}