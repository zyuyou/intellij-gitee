// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.util

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
 */
class GiteeGitHelper(private val githubSettings: com.gitee.util.GiteeSettings,
                     private val authenticationManager: GiteeAuthenticationManager) {
//                     private val migrationHelper: GiteeAccountsMigrationHelper) {

  private val DEFAULT_SERVER = GiteeServerPath(host = GiteeServerPath.DEFAULT_HOST)

  fun getRemoteUrl(server: GiteeServerPath, fullName: String): String {
    return if (githubSettings.isCloneGitUsingSsh) {
      "git@${server.host}:${server.suffix?.substring(1).orEmpty()}/$fullName.git"
    } else {
      "https://${server.host}${server.suffix.orEmpty()}/$fullName.git"
    }
  }

  fun getRemoteUrl(server: GiteeServerPath, repoPath: com.gitee.api.GiteeFullPath): String {
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

//    migrationHelper.getOldServer()?.run(registeredServers::add)

    authenticationManager.getAccounts().mapTo(registeredServers) { it.server }
    val repositoryPaths = mutableSetOf<GiteeRepositoryPath>()

    for (url in repository.remotes.map { it.urls }.flatten()) {
      registeredServers.filter { it.matches(url) }
        .mapNotNullTo(repositoryPaths) { server ->
          com.gitee.util.GiteeUrlUtil.getUserAndRepositoryFromRemoteUrl(url)?.let { GiteeRepositoryPath(server, it) }
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