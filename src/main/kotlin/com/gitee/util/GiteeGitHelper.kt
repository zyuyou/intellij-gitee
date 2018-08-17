/*
 * Copyright 2016-2018 码云 - Gitee
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gitee.util

import com.gitee.api.GiteeFullPath
import com.gitee.api.GiteeRepositoryPath
import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GiteeAuthenticationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import git4idea.GitUtil
import git4idea.repo.GitRepository

/**
 * Utilities for Github-Git interactions
 *
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/util/GithubGitHelper.java
 * @author JetBrains s.r.o.
 */
class GiteeGitHelper(private val githubSettings: GiteeSettings,
                     private val authenticationManager: GiteeAuthenticationManager) {

  private val DEFAULT_SERVER = GiteeServerPath(host = GiteeServerPath.DEFAULT_HOST)

  fun getRemoteUrl(server: GiteeServerPath, fullName: String): String {
    return if (githubSettings.isCloneGitUsingSsh) {
      "git@${server.host}:${server.suffix?.substring(1).orEmpty()}/$fullName.git"
    } else {
      "https://${server.host}${server.suffix.orEmpty()}/$fullName.git"
    }
  }

  fun getRemoteUrl(server: GiteeServerPath, repoPath: GiteeFullPath): String {
    return getRemoteUrl(server, repoPath.user, repoPath.repository)
  }

  fun getRemoteUrl(server: GiteeServerPath, user: String, repo: String): String {
    return if (githubSettings.isCloneGitUsingSsh) {
      "git@${server.host}:${server.suffix?.substring(1).orEmpty()}/$user/$repo.git"
    } else {
      "https://${server.host}${server.suffix.orEmpty()}/$user/$repo.git"
    }
  }

  fun getAccessibleRemoteUrls(repository: GitRepository): List<String> {
    return repository.remotes.map { it.urls }.flatten().filter(::isRemoteUrlAccessible)
  }

  fun hasAccessibleRemotes(repository: GitRepository): Boolean {
    return repository.remotes.map { it.urls }.flatten().any(::isRemoteUrlAccessible)
  }

  private fun isRemoteUrlAccessible(url: String) = authenticationManager.getAccounts().find { it.server.matches(url) } != null

  fun getPossibleRepositories(repository: GitRepository): Set<GiteeRepositoryPath> {
    val registeredServers = mutableSetOf(DEFAULT_SERVER)

    authenticationManager.getAccounts().mapTo(registeredServers) { it.server }
    val repositoryPaths = mutableSetOf<GiteeRepositoryPath>()

    for (url in repository.remotes.map { it.urls }.flatten()) {
      registeredServers.filter { it.matches(url) }
        .mapNotNullTo(repositoryPaths) { server ->
          GiteeUrlUtil.getUserAndRepositoryFromRemoteUrl(url)?.let { GiteeRepositoryPath(server, it) }
        }
    }
    return repositoryPaths
  }

  companion object {
    @JvmStatic
    fun findGitRepository(project: Project, file: VirtualFile?): GitRepository? {
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