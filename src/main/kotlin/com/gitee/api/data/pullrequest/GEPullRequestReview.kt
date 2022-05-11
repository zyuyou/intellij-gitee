// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.pullrequest

import com.gitee.api.data.GEActor
import com.gitee.api.data.GENode
import com.gitee.api.data.pullrequest.timeline.GEPRTimelineItem
import com.intellij.collaboration.api.dto.GraphQLFragment
import java.util.*

@GraphQLFragment("/graphql/fragment/pullRequestReview.graphql")
open class GEPullRequestReview(id: String,
                               val url: String,
                               val author: GEActor?,
                               val body: String,
                               val state: GEPullRequestReviewState,
                               val createdAt: Date,
                               val viewerCanUpdate: Boolean)
  : GENode(id), GEPRTimelineItem