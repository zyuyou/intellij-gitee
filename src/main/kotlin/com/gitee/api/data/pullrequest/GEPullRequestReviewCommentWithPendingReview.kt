// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.pullrequest

import com.fasterxml.jackson.annotation.JsonProperty
import com.gitee.api.data.GEActor
import com.gitee.api.data.GECommitHash
import com.gitee.api.data.GENode
import java.util.*

open class GEPullRequestReviewCommentWithPendingReview(id: String,
                                                       databaseId: Long,
                                                       url: String,
                                                       author: GEActor?,
                                                       body: String,
                                                       createdAt: Date,
                                                       state: GEPullRequestReviewCommentState,
                                                       commit: GECommitHash?,
                                                       originalCommit: GECommitHash?,
                                                       replyTo: GENode?,
                                                       diffHunk: String,
                                                       @JsonProperty("pullRequestReview") val pullRequestReview: GEPullRequestPendingReview,
                                                       viewerCanDelete: Boolean,
                                                       viewerCanUpdate: Boolean)
  : GEPullRequestReviewComment(id, databaseId, url, author, body, createdAt, state, commit, originalCommit,
                               replyTo, diffHunk, pullRequestReview, viewerCanDelete, viewerCanUpdate) {
}
