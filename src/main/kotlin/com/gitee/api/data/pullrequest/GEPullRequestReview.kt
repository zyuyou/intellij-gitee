// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.pullrequest

import com.gitee.api.data.GEActor
import com.gitee.api.data.GENode
import com.gitee.api.data.pullrequest.timeline.GiteePRTimelineItem
import java.util.*

open class GEPullRequestReview(id: String,
                               val url: String,
                               val author: GEActor?,
                               val bodyHTML: String,
                               val state: GiteePullRequestReviewState,
                               val createdAt: Date)
  : GENode(id), GiteePRTimelineItem