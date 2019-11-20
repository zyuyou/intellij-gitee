// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.pullrequest

import com.fasterxml.jackson.annotation.JsonProperty
import com.gitee.api.data.GENode
import com.gitee.api.data.GENodes

class GEPullRequestReviewThread(id: String,
                                val isResolved: Boolean,
                                @JsonProperty("comments") comments: GENodes<GEPullRequestReviewComment>)
  : GENode(id) {
  val comments = comments.nodes
  private val root = comments.nodes.first()

  val path = root.path
  val commit = root.commit
  val position = root.position
  val originalCommit = root.originalCommit
  val originalPosition = root.originalPosition
  val createdAt = root.createdAt
  val diffHunk = root.diffHunk

  val reviewId = root.reviewId
  val firstCommentDatabaseId = root.databaseId
}
