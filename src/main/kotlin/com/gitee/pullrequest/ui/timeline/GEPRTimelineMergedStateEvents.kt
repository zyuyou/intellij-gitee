// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.timeline

import com.gitee.api.data.pullrequest.GEPullRequestState
import com.gitee.api.data.pullrequest.timeline.GEPRTimelineEvent


class GEPRTimelineMergedStateEvents(initialState: GEPRTimelineEvent.State) : GEPRTimelineMergedEvents<GEPRTimelineEvent.State>(), GEPRTimelineEvent.State {
  private val inferredOriginalState: GEPullRequestState = when (initialState.newState) {
    GEPullRequestState.CLOSED -> GEPullRequestState.OPEN
    GEPullRequestState.MERGED -> GEPullRequestState.OPEN
    GEPullRequestState.OPEN -> GEPullRequestState.CLOSED
  }

  init {
    add(initialState)
  }

  override var newState: GEPullRequestState = initialState.newState
    private set

  var lastStateEvent = initialState
    private set

  override fun addNonMergedEvent(event: GEPRTimelineEvent.State) {
    if (newState != GEPullRequestState.MERGED) {
      newState = event.newState
      lastStateEvent = event
    }
  }

  override fun hasAnyChanges(): Boolean = newState != inferredOriginalState
}