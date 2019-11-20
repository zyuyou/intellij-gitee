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
package com.gitee.issue

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequests
import com.gitee.api.GiteeRepositoryPath
import com.gitee.api.GiteeServerPath
import com.gitee.api.data.GiteeIssue
import com.gitee.api.data.GiteeSearchedIssue
import com.gitee.api.util.GiteeApiPagesLoader
import com.intellij.openapi.progress.ProgressIndicator
import java.io.IOException

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/issue/GithubIssuesLoadingHelper.kt
 * @author JetBrains s.r.o.
 */
object GiteeIssuesLoadingHelper {
  @JvmOverloads
  @JvmStatic
  @Throws(IOException::class)
  fun load(executor: GiteeApiRequestExecutor, indicator: ProgressIndicator, server: GiteeServerPath,
           owner: String, repo: String, withClosed: Boolean, maximum: Int = 100, assignee: String? = null): List<GiteeIssue> {
    return GiteeApiPagesLoader.load(
      executor,
      indicator,
      GiteeApiRequests.Repos.Issues.pages(server, owner, repo, if (withClosed) "all" else "open", assignee),
      maximum
    )
  }

  @JvmOverloads
  @JvmStatic
  @Throws(IOException::class)
  fun search(executor: GiteeApiRequestExecutor, indicator: ProgressIndicator, server: GiteeServerPath,
             owner: String, repo: String, withClosed: Boolean, assignee: String? = null, query: String? = null)
    : List<GiteeSearchedIssue> {

    return GiteeApiPagesLoader.loadAll(executor, indicator,
      GiteeApiRequests.Search.Issues.pages(
        server,
        GiteeRepositoryPath(owner, repo),
        if (withClosed) null else "open", assignee, query)
    )
  }
}