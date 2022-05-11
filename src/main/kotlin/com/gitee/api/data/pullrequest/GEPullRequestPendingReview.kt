// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.pullrequest

import com.gitee.api.data.GENode
import com.gitee.api.data.GENodes
import com.intellij.collaboration.api.dto.GraphQLFragment

@GraphQLFragment("/graphql/fragment/pullRequestPendingReview.graphql")
open class GEPullRequestPendingReview(id: String,
                                      val state: GEPullRequestReviewState,
                                      val comments: GENodes<GEPullRequestReviewComment>
)
  : GENode(id)