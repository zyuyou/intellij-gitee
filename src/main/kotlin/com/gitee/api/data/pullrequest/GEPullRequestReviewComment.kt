// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.pullrequest

import com.fasterxml.jackson.annotation.JsonProperty
import com.gitee.api.data.GEActor
import com.gitee.api.data.GEComment
import com.gitee.api.data.GECommitHash
import com.gitee.api.data.GENode
import java.util.*

class GEPullRequestReviewComment(id: String,
                                 val databaseId: Long,
                                 val url: String,
                                 author: GEActor?,
                                 bodyHTML: String,
                                 createdAt: Date,
                                 val path: String,
                                 val commit: GECommitHash,
                                 val position: Int?,
                                 val originalCommit: GECommitHash?,
                                 val originalPosition: Int,
                                 val replyTo: GENode?,
                                 val diffHunk: String,
                                 @JsonProperty("pullRequestReview") pullRequestReview: GENode)
  : GEComment(id, author, bodyHTML, createdAt) {
  val reviewId = pullRequestReview.id
}