// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.pullrequest.timeline

import com.gitee.api.data.GEActor
import com.gitee.api.data.GECommitShort
import com.gitee.api.data.pullrequest.GEPullRequestState
import java.util.*

class GEPRMergedEvent(override val actor: GEActor?,
                      override val createdAt: Date,
                      val commit: GECommitShort?,
                      val mergeRefName: String)
  : GEPRTimelineEvent.State {
  override val newState = GEPullRequestState.MERGED
}