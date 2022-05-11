// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data.service

import com.gitee.api.data.GEBranchProtectionRules
import com.gitee.pullrequest.data.GEPRIdentifier
import com.gitee.pullrequest.data.GEPRMergeabilityState
import com.intellij.openapi.progress.ProgressIndicator
import org.jetbrains.annotations.CalledInAny
import java.util.concurrent.CompletableFuture

interface GEPRStateService {

  @CalledInAny
  fun loadBranchProtectionRules(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier, baseBranch: String)
    : CompletableFuture<GEBranchProtectionRules?>

  @CalledInAny
  fun loadMergeabilityState(progressIndicator: ProgressIndicator,
                            pullRequestId: GEPRIdentifier,
                            headRefOid: String,
                            prHtmlUrl: String,
                            baseBranchProtectionRules: GEBranchProtectionRules?): CompletableFuture<GEPRMergeabilityState>


  @CalledInAny
  fun close(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier): CompletableFuture<Unit>

  @CalledInAny
  fun reopen(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier): CompletableFuture<Unit>

  @CalledInAny
  fun markReadyForReview(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier): CompletableFuture<Unit>

  @CalledInAny
  fun merge(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier,
            commitMessage: Pair<String, String>, currentHeadRef: String): CompletableFuture<Unit>

  @CalledInAny
  fun rebaseMerge(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier,
                  currentHeadRef: String): CompletableFuture<Unit>

  @CalledInAny
  fun squashMerge(progressIndicator: ProgressIndicator, pullRequestId: GEPRIdentifier,
                  commitMessage: Pair<String, String>, currentHeadRef: String): CompletableFuture<Unit>
}
