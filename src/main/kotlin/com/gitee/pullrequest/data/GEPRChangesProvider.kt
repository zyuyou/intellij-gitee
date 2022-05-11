// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data

import com.intellij.openapi.vcs.changes.Change

interface GEPRChangesProvider {
  val changes: List<Change>
  val changesByCommits: Map<String, List<Change>>
  val linearHistory: Boolean

  fun findChangeDiffData(change: Change): GEPRChangeDiffData?

  fun findCumulativeChange(commitSha: String, filePath: String): Change?
}