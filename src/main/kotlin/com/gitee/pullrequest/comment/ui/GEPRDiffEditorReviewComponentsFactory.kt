// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.comment.ui

import com.intellij.diff.util.Side
import javax.swing.JComponent

interface GEPRDiffEditorReviewComponentsFactory {
  fun createThreadComponent(thread: GEPRReviewThreadModel): JComponent
  fun createSingleCommentComponent(side: Side, line: Int, startLine: Int = line, hideCallback: () -> Unit): JComponent
  fun createNewReviewCommentComponent(side: Side, line: Int, startLine: Int = line, hideCallback: () -> Unit): JComponent
  fun createReviewCommentComponent(reviewId: String, side: Side, line: Int, startLine: Int = line, hideCallback: () -> Unit): JComponent
}