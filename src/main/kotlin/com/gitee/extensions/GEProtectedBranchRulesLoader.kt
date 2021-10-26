// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.extensions

import com.gitee.api.GiteeApiRequestExecutorManager
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.i18n.GiteeBundle
import com.gitee.util.GEProjectRepositoriesManager
import com.gitee.util.GiteeProjectSettings
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import git4idea.config.GitSharedSettings
import git4idea.fetch.GitFetchHandler
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository

private val LOG = logger<GEProtectedBranchRulesLoader>()

internal class GEProtectedBranchRulesLoader : GitFetchHandler {

  override fun doAfterSuccessfulFetch(project: Project, fetches: Map<GitRepository, List<GitRemote>>, indicator: ProgressIndicator) {
    try {
      loadProtectionRules(indicator, fetches, project)
    }
    catch (e: Exception) {
      if (e is ProcessCanceledException) {
        throw e
      }
      LOG.info("Error occurred while trying to load branch protection rules", e)
    }
  }

  private fun loadProtectionRules(indicator: ProgressIndicator,
                                  fetches: Map<GitRepository, List<GitRemote>>,
                                  project: Project) {

    val giteeAuthenticationManager = GiteeAuthenticationManager.getInstance()

    if (!GitSharedSettings.getInstance(project).isSynchronizeBranchProtectionRules || !giteeAuthenticationManager.hasAccounts()) {
      runInEdt {
        project.service<GiteeProjectSettings>().branchProtectionPatterns = arrayListOf()
      }
      return
    }

    indicator.text = GiteeBundle.message("progress.text.loading.protected.branches")

    val branchProtectionPatterns = mutableSetOf<String>()
    for ((repository, remotes) in fetches) {
      indicator.checkCanceled()

      for (remote in remotes) {
        indicator.checkCanceled()

        val account =
          giteeAuthenticationManager.getAccounts().find { it.server.matches(remote.firstUrl.orEmpty()) } ?: continue

        val requestExecutor = GiteeApiRequestExecutorManager.getInstance().getExecutor(account)

        val giteeRepositoryMapping =
          project.service<GEProjectRepositoriesManager>().findKnownRepositories(repository).find {
            it.gitRemoteUrlCoordinates.remote == remote
          } ?: continue

        val repositoryCoordinates = giteeRepositoryMapping.geRepositoryCoordinates

//        SimpleGHGQLPagesLoader(requestExecutor, { GHGQLRequests.Repo.getProtectionRules(repositoryCoordinates) })
//          .loadAll(SensitiveProgressWrapper((indicator)))
//          .forEach { rule -> branchProtectionPatterns.add(PatternUtil.convertToRegex(rule.pattern)) }
      }

    }

    runInEdt {
      project.service<GiteeProjectSettings>().branchProtectionPatterns = branchProtectionPatterns.toMutableList()
    }
  }

}
