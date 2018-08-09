// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.issue

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequests
import com.gitee.api.GiteeServerPath
import com.gitee.api.util.GiteeApiPagesLoader
import com.intellij.openapi.progress.ProgressIndicator
import java.io.IOException

object GiteeIssuesLoadingHelper {
  @JvmOverloads
  @JvmStatic
  @Throws(IOException::class)
  fun load(executor: GiteeApiRequestExecutor, indicator: ProgressIndicator, server: GiteeServerPath,
           owner: String, repo: String, withClosed: Boolean, maximum: Int = 100, assignee: String? = null): List<com.gitee.api.data.GiteeIssue> {
    return GiteeApiPagesLoader.load(
      executor,
      indicator,
      GiteeApiRequests.Repos.Issues.pages(server, owner, repo, if (withClosed) "all" else "open", assignee),
      maximum
    )
  }

//  @JvmOverloads
//  @JvmStatic
//  @Throws(IOException::class)
//  fun search(executor: GiteeApiRequestExecutor, indicator: ProgressIndicator, server: GiteeServerPath,
//             owner: String, repo: String, withClosed: Boolean, assignee: String? = null, query: String? = null)
//    : List<GiteeIssue> {
//
//    return GiteeApiPagesLoader.loadAll(executor, indicator,
//      GiteeApiRequests.Search.Issues.pages(server,
//        GiteeFullPath(owner, repo),
//        if (withClosed) null else "open", assignee, query))
//  }
}