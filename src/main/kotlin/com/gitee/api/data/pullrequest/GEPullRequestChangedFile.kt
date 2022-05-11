// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.pullrequest

enum class GEPullRequestFileViewedState {
  DISMISSED, UNVIEWED, VIEWED
}

class GEPullRequestChangedFile(
  val path: String,
  val viewerViewedState: GEPullRequestFileViewedState
)

internal fun GEPullRequestFileViewedState.isViewed(): Boolean = this == GEPullRequestFileViewedState.VIEWED