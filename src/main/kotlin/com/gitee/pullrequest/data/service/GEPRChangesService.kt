// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data.service

import com.gitee.api.data.GECommit
import com.gitee.pullrequest.data.GEPRChangesProvider
import com.gitee.pullrequest.data.GEPRIdentifier
import com.google.common.graph.Graph
import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.annotations.CalledInAny
import java.util.concurrent.CompletableFuture

interface GEPRChangesService {

  @CalledInAny
  fun fetch(progressIndicator: ProgressIndicator, refspec: String): CompletableFuture<Unit>

  @CalledInAny
  fun fetchBranch(progressIndicator: ProgressIndicator, branch: String): CompletableFuture<Unit>

  @CalledInAny
  fun loadCommitsFromApi(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier)
    : CompletableFuture<Pair<GECommit, Graph<GECommit>>>

  @CalledInAny
  fun loadCommitDiffs(progressIndicator: ProgressIndicator, baseRefOid: String, oid: String): CompletableFuture<Pair<String, String>>

  @CalledInAny
  fun loadMergeBaseOid(progressIndicator: ProgressIndicator, baseRefOid: String, headRefOid: String): CompletableFuture<String>

  @CalledInAny
  fun createChangesProvider(progressIndicator: ProgressIndicator, mergeBaseOid: String, commits: Pair<GECommit, Graph<GECommit>>)
    : CompletableFuture<GEPRChangesProvider>
}