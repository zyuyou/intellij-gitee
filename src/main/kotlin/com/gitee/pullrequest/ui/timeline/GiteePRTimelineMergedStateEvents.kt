// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.timeline

import com.gitee.api.data.pullrequest.GiteePullRequestState
import com.gitee.api.data.pullrequest.timeline.GiteePRTimelineEvent

class GiteePRTimelineMergedStateEvents(initialState: GiteePRTimelineEvent.State) : GiteePRTimelineMergedEvents<GiteePRTimelineEvent.State>(), GiteePRTimelineEvent.State {
  private val inferredOriginalState: GiteePullRequestState = when (initialState.newState) {
    GiteePullRequestState.CLOSED -> GiteePullRequestState.OPEN
    GiteePullRequestState.MERGED -> GiteePullRequestState.OPEN
    GiteePullRequestState.OPEN -> GiteePullRequestState.CLOSED
  }

  override var newState: GiteePullRequestState = initialState.newState
    private set

  override fun addNonMergedEvent(event: GiteePRTimelineEvent.State) {
    newState = event.newState
  }

  override fun hasAnyChanges(): Boolean = newState != inferredOriginalState
}