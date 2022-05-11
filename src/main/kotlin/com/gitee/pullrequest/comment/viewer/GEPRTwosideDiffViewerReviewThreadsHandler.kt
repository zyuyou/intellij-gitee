// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.comment.viewer

import com.gitee.pullrequest.comment.GEPRCommentsUtil
import com.gitee.pullrequest.comment.GEPRDiffReviewThreadMapping
import com.gitee.pullrequest.comment.ui.*
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.collaboration.ui.codereview.diff.EditorComponentInlaysManager
import com.intellij.diff.tools.util.side.TwosideTextDiffViewer
import com.intellij.diff.util.LineRange
import com.intellij.diff.util.Range
import com.intellij.diff.util.Side
import com.intellij.openapi.editor.impl.EditorImpl

class GEPRTwosideDiffViewerReviewThreadsHandler(reviewProcessModel: GEPRReviewProcessModel,
                                                commentableRangesModel: SingleValueModel<List<Range>?>,
                                                reviewThreadsModel: SingleValueModel<List<GEPRDiffReviewThreadMapping>?>,
                                                viewer: TwosideTextDiffViewer,
                                                componentsFactory: GEPRDiffEditorReviewComponentsFactory,
                                                cumulative: Boolean)
  : GEPRDiffViewerBaseReviewThreadsHandler<TwosideTextDiffViewer>(commentableRangesModel, reviewThreadsModel, viewer) {

  private val commentableRangesLeft = SingleValueModel<List<LineRange>>(emptyList())
  private val editorThreadsLeft = GEPREditorReviewThreadsModel()

  private val commentableRangesRight = SingleValueModel<List<LineRange>>(emptyList())
  private val editorThreadsRight = GEPREditorReviewThreadsModel()

  override val viewerReady = true

  init {
    val inlaysManagerLeft = EditorComponentInlaysManager(viewer.editor1 as EditorImpl)

    val gutterIconRendererFactoryLeft = GEPRDiffEditorGutterIconRendererFactoryImpl(reviewProcessModel,
                                                                                    inlaysManagerLeft,
                                                                                    componentsFactory,
                                                                                    cumulative) { line ->
      val (startLine, endLine) = getCommentLinesRange(viewer.editor1, line)
      GEPRCommentLocation(Side.LEFT, endLine, startLine)
    }

    GEPREditorCommentableRangesController(commentableRangesLeft, gutterIconRendererFactoryLeft, viewer.editor1)
    GEPREditorReviewThreadsController(editorThreadsLeft, componentsFactory, inlaysManagerLeft)

    val inlaysManagerRight = EditorComponentInlaysManager(viewer.editor2 as EditorImpl)

    val gutterIconRendererFactoryRight = GEPRDiffEditorGutterIconRendererFactoryImpl(reviewProcessModel,
                                                                                     inlaysManagerRight,
                                                                                     componentsFactory,
                                                                                     cumulative) { line ->
      val (startLine, endLine) = getCommentLinesRange(viewer.editor2, line)
      GEPRCommentLocation(Side.RIGHT, endLine, startLine)
    }

    GEPREditorCommentableRangesController(commentableRangesRight, gutterIconRendererFactoryRight, viewer.editor2)
    GEPREditorReviewThreadsController(editorThreadsRight, componentsFactory, inlaysManagerRight)
  }

  override fun markCommentableRanges(ranges: List<Range>?) {
    commentableRangesLeft.value = ranges?.let { GEPRCommentsUtil.getLineRanges(it, Side.LEFT) }.orEmpty()
    commentableRangesRight.value = ranges?.let { GEPRCommentsUtil.getLineRanges(it, Side.RIGHT) }.orEmpty()
  }

  override fun showThreads(threads: List<GEPRDiffReviewThreadMapping>?) {
    editorThreadsLeft.update(threads
                               ?.filter { it.diffSide == Side.LEFT }
                               ?.groupBy({ it.fileLineIndex }, { it.thread }).orEmpty())
    editorThreadsRight.update(threads
                                ?.filter { it.diffSide == Side.RIGHT }
                                ?.groupBy({ it.fileLineIndex }, { it.thread }).orEmpty())
  }
}
