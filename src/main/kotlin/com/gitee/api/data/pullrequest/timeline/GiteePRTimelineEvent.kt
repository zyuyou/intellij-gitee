// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.pullrequest.timeline

import com.gitee.api.data.GEActor
import com.gitee.api.data.pullrequest.GiteePullRequestState
import java.util.*

interface GiteePRTimelineEvent : GiteePRTimelineItem {
  val actor: GEActor?
  val createdAt: Date

  /**
   * Simple events which can be merged together
   */
  interface Simple : GiteePRTimelineEvent

  /**
   * Events about pull request state
   */
  interface State : GiteePRTimelineEvent {
    val newState: GiteePullRequestState
  }

  /**
   * More complex events which can NOT be merged together
   */
  interface Complex : GiteePRTimelineEvent

  /**
   * Pull request head/base branch changes events
   */
  interface Branch : Complex
}