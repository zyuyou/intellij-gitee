// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.comment.ui

import com.gitee.api.data.GECommitHash
import com.gitee.api.data.pullrequest.GEPullRequestReviewCommentState
import com.gitee.api.data.pullrequest.GEPullRequestReviewThread
import java.util.*
import javax.swing.ListModel

interface GEPRReviewThreadModel : ListModel<GEPRReviewCommentModel> {
  val id: String
  val createdAt: Date
  val state: GEPullRequestReviewCommentState
  val isResolved: Boolean
  val isOutdated: Boolean
  val commit: GECommitHash?
  val filePath: String
  val diffHunk: String
  val line: Int
  val startLine: Int?

  fun update(thread: GEPullRequestReviewThread)
  fun addComment(comment: GEPRReviewCommentModel)

  fun addAndInvokeStateChangeListener(listener: () -> Unit)
}
