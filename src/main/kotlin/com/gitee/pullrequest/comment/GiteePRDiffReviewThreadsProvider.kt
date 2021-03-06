// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.comment

import com.intellij.diff.tools.util.base.DiffViewerBase
import com.intellij.openapi.util.Key
import com.intellij.openapi.vcs.changes.Change
import org.jetbrains.annotations.CalledInAwt

interface GiteePRDiffReviewThreadsProvider {
  @CalledInAwt
  fun install(viewer: DiffViewerBase, change: Change)

  companion object {
    val KEY = Key.create<GiteePRDiffReviewThreadsProvider>("Gitee.PullRequest.Diff.Comments")
  }
}