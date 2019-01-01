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
package com.gitee.pullrequest.data

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequests
import com.gitee.api.GiteeFullPath
import com.gitee.api.GiteeServerPath
import com.gitee.api.data.GiteeIssue
import com.gitee.api.data.GiteePullRequest
import com.gitee.api.data.GiteeResponsePage
import com.gitee.api.requests.GiteeRequestPagination
import com.gitee.api.util.GiteeApiSearchQueryBuilder
import com.gitee.api.util.GiteeApiUrlQueryBuilder
import com.gitee.pullrequest.search.GiteePullRequestSearchQuery
import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.Computable
import org.jetbrains.annotations.CalledInAwt
import java.util.concurrent.CompletableFuture

/**
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/pullrequest/data/GithubPullRequestsLoader.kt
 * @author JetBrains s.r.o.
 */
internal class GiteePullRequestsLoader(private val progressManager: ProgressManager,
                                       private val requestExecutor: GiteeApiRequestExecutor,
                                       private val serverPath: GiteeServerPath,
                                       private val repoPath: GiteeFullPath) : Disposable {
//  private var initialRequest = GiteeApiRequests.Search.Issues.get(serverPath, buildQuery(null))
//  private var initialRequest = GiteeApiRequests.Repos.Issues.get(serverPath, repoPath.user, repoPath.repository)
  private var initialRequest = GiteeApiRequests.Repos.PullRequests.get(serverPath, repoPath.user, repoPath.repository)
  private var lastFuture = CompletableFuture.completedFuture(GiteeResponsePage.empty<GiteePullRequest>(initialRequest.url))

  @CalledInAwt
  fun setSearchQuery(searchQuery: GiteePullRequestSearchQuery?) {
//    initialRequest = GiteeApiRequests.Search.Issues.get(serverPath, buildQuery(searchQuery))
//    initialRequest = GiteeApiRequests.Repos.Issues.get(serverPath, buildQuery(null))
    initialRequest = GiteeApiRequests.Repos.PullRequests.get(serverPath, repoPath.user, repoPath.repository, buildQuery(searchQuery))
  }

  private fun buildQuery(searchQuery: GiteePullRequestSearchQuery?): String {
//    return GiteeApiSearchQueryBuilder.searchQuery {
//      qualifier("type", GiteeIssueSearchType.pr.name)
//      qualifier("owner", repoPath.user)
//      qualifier("repo", repoPath.fullName)
//      searchQuery?.buildApiSearchQuery(this)
//    }
    return GiteeApiUrlQueryBuilder.urlQuery {
      searchQuery?.buildApiSearchQuery(this)
      param(GiteeRequestPagination())
    }
  }

  @CalledInAwt
  fun requestLoadMore(indicator: ProgressIndicator): CompletableFuture<GiteeResponsePage<GiteePullRequest>> {
    lastFuture = lastFuture.thenApplyAsync {
      it.nextLink?.let { url ->
        progressManager.runProcess(Computable { requestExecutor.execute(indicator, GiteeApiRequests.Repos.PullRequests.get(url)) }, indicator)
      } ?: GiteeResponsePage.empty()
    }
    return lastFuture
  }

  @CalledInAwt
  fun reset() {
    lastFuture = lastFuture.handle { _, _ ->
      GiteeResponsePage.empty<GiteePullRequest>(initialRequest.url)
    }
  }

  override fun dispose() {
    reset()
  }
}