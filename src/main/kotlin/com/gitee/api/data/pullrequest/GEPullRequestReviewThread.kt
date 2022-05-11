// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.pullrequest

import com.fasterxml.jackson.annotation.JsonProperty
import com.gitee.api.data.GENode
import com.gitee.api.data.GENodes
import com.intellij.collaboration.api.dto.GraphQLFragment
import com.intellij.diff.util.Side

@GraphQLFragment("/graphql/fragment/pullRequestReviewThread.graphql")
class GEPullRequestReviewThread(id: String,
                                val isResolved: Boolean,
                                val isOutdated: Boolean,
                                val path: String,
                                @JsonProperty("diffSide") val side: Side,
                                val line: Int,
                                val startLine: Int?,
                                @JsonProperty("comments") comments: GENodes<GEPullRequestReviewComment>)
  : GENode(id) {
  val comments = comments.nodes
  private val root = comments.nodes.first()

  val state = root.state
  val commit = root.commit
  val originalCommit = root.originalCommit
  val createdAt = root.createdAt
  val diffHunk = root.diffHunk
  val reviewId = root.reviewId
}