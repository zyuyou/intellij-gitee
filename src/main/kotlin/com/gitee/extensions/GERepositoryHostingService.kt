// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.extensions

import com.gitee.extensions.GEHttpAuthDataProvider.Companion.getGitAuthenticationAccounts
import com.gitee.util.GiteeUtil
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import git4idea.remote.GitRepositoryHostingService
import git4idea.remote.InteractiveGitHttpAuthDataProvider

internal class GERepositoryHostingService : GitRepositoryHostingService() {
  override fun getServiceDisplayName(): String = GiteeUtil.SERVICE_DISPLAY_NAME

//  override fun getRepositoryListLoader(project: Project): RepositoryListLoader = GHRepositoryListLoader(project)

  @RequiresBackgroundThread
  override fun getInteractiveAuthDataProvider(project: Project, url: String): InteractiveGitHttpAuthDataProvider? =
    getProvider(project, url, null)

  @RequiresBackgroundThread
  override fun getInteractiveAuthDataProvider(project: Project, url: String, login: String): InteractiveGitHttpAuthDataProvider? =
    getProvider(project, url, login)

  private fun getProvider(project: Project, url: String, login: String?): InteractiveGitHttpAuthDataProvider? {
    val accounts = getGitAuthenticationAccounts(project, url, login)

    return if (accounts.isNotEmpty()) GESelectAccountHttpAuthDataProvider(project, accounts) else null
  }
}

//private class GHRepositoryListLoader(private val project: Project) : RepositoryListLoader {
//  private val authenticationManager get() = GiteeAuthenticationManager.getInstance()
//  private val executorManager get() = GiteeApiRequestExecutorManager.getInstance()
//  private val gitHelper get() = GiteeGitHelper.getInstance()
//
//  private val executors = mutableMapOf<GiteeAccount, GiteeApiRequestExecutor>()
//
//  override fun isEnabled(): Boolean {
//    authenticationManager.getAccounts().forEach { account ->
//      try {
//        executors[account] = executorManager.getExecutor(account)
//      }
//      catch (ignored: GiteeMissingTokenException) {
//      }
//    }
//    return executors.isNotEmpty()
//  }
//
//  override fun enable(parentComponent: Component?): Boolean {
//    if (!authenticationManager.ensureHasAccounts(project, parentComponent)) return false
//
//    var atLeastOneHasToken = false
//    for (account in authenticationManager.getAccounts()) {
//      val executor = executorManager.getExecutor(account, project) ?: continue
//      executors[account] = executor
//      atLeastOneHasToken = true
//    }
//    return atLeastOneHasToken
//  }
//
//  override fun getAvailableRepositoriesFromMultipleSources(progressIndicator: ProgressIndicator): RepositoryListLoader.Result {
//    val urls = mutableListOf<String>()
//    val exceptions = mutableListOf<RepositoryListLoadingException>()
//
//    executors.forEach { (account, executor) ->
//      try {
//        val associatedRepos = account.loadAssociatedRepos(executor, progressIndicator)
//        // We already can return something useful from getUserRepos, so let's ignore errors.
//        // One of this may not exist in GitHub enterprise
//        val watchedRepos = account.loadWatchedReposSkipErrors(executor, progressIndicator)
//
//        urls.addAll(
//          (associatedRepos + watchedRepos)
//            .sortedWith(compareBy({ repo -> repo.userName }, { repo -> repo.name }))
//            .map { repo -> gitHelper.getRemoteUrl(account.server, repo.userName, repo.name) }
//        )
//      }
//      catch (e: Exception) {
//        exceptions.add(RepositoryListLoadingException("Cannot load repositories from GitHub", e))
//      }
//    }
//
//    return RepositoryListLoader.Result(urls, exceptions)
//  }
//}

//private fun GiteeAccount.loadAssociatedRepos(executor: GiteeApiRequestExecutor, indicator: ProgressIndicator): List<GiteeRepo> =
//  loadAll(executor, indicator, GiteeApiRequests.CurrentUser.Repos.pages(server))
//
//private fun GiteeAccount.loadWatchedReposSkipErrors(executor: GiteeApiRequestExecutor, indicator: ProgressIndicator): List<GiteeRepo> =
//  try {
//    loadAll(executor, indicator, GiteeApiRequests.CurrentUser.RepoSubs.pages(server))
//  }
//  catch (e: GiteeAuthenticationException) {
//    emptyList()
//  }
//  catch (e: GiteeStatusCodeException) {
//    emptyList()
//  }