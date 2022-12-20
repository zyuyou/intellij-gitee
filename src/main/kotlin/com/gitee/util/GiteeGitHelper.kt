/*
 *  Copyright 2016-2019 码云 - Gitee
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.gitee.util

import com.gitee.api.GERepositoryPath
import com.gitee.api.GiteeServerPath
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import git4idea.GitUtil
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository

/**
 * Utilities for Gitee-Git interactions
 *
 * accessible url - url that matches at least one registered account
 * possible url - accessible urls + urls that match gitee.com + urls that match server saved in old settings
 *
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/util/GiteeGitHelper.java
 * @author JetBrains s.r.o.
 */
@Service
class GiteeGitHelper {
  fun getRemoteUrl(server: GiteeServerPath, repoPath: GERepositoryPath): String {
    return getRemoteUrl(server, repoPath.owner, repoPath.repository)
  }

  fun getRemoteUrl(server: GiteeServerPath, fullName: String): String {
    return if (GiteeSettings.getInstance().isCloneGitUsingSsh) {
      "git@${server.host}:${server.suffix?.substring(1).orEmpty()}/$fullName.git"
    } else {
      "https://${server.host}${server.suffix.orEmpty()}/$fullName.git"
    }
  }

  fun getRemoteUrl(server: GiteeServerPath, user: String, repo: String): String {
    return if (GiteeSettings.getInstance().isCloneGitUsingSsh) {
      "git@${server.host}:${server.suffix?.substring(1).orEmpty()}/$user/$repo.git"
    } else {
      "https://${server.host}${server.suffix.orEmpty()}/$user/$repo.git"
    }
  }

  fun findRemote(repository: GitRepository, httpUrl: String?, sshUrl: String?): GitRemote? =
    repository.remotes.find {
      it.firstUrl != null && (it.firstUrl == httpUrl ||
              it.firstUrl == httpUrl + GitUtil.DOT_GIT ||
              it.firstUrl == sshUrl ||
              it.firstUrl == sshUrl + GitUtil.DOT_GIT)
    }

  fun findLocalBranch(repository: GitRepository, prRemote: GitRemote, isFork: Boolean, possibleBranchName: String?): String? {
    val localBranchesWithTracking =
      with(repository.branches) {
        if (isFork) {
          localBranches.filter { it.findTrackedBranch(repository)?.remote == prRemote }
        }
        else {
          val prRemoteBranch = remoteBranches.find { it.nameForRemoteOperations == possibleBranchName } ?: return null
          localBranches.filter { it.findTrackedBranch(repository) == prRemoteBranch }
        }
      }
    return localBranchesWithTracking.find { it.name == possibleBranchName }?.name
    // if PR was made not from fork we can assume that the first local branch with tracking to that fork is a good candidate of local branch for that PR.
      ?: if (!isFork) localBranchesWithTracking.firstOrNull()?.name else null
  }

  companion object {
    @JvmStatic
    fun findGitRepository(project: Project, file: VirtualFile? = null): GitRepository? {
      val manager = GitUtil.getRepositoryManager(project)
      val repositories = manager.repositories

      if (repositories.size == 0) {
        return null
      }
      if (repositories.size == 1) {
        return repositories[0]
      }

      if (file != null) {
        val repository = manager.getRepositoryForFile(file)
        if (repository != null) {
          return repository
        }
      }
      return manager.getRepositoryForFile(project.baseDir)
    }

    @JvmStatic
    fun getInstance(): GiteeGitHelper {
      return service()
    }
  }
}