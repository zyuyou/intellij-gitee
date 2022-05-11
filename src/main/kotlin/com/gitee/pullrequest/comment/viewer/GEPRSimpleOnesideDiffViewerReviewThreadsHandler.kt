// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.comment.viewer

import com.gitee.pullrequest.comment.GEPRCommentsUtil
import com.gitee.pullrequest.comment.GEPRDiffReviewThreadMapping
import com.gitee.pullrequest.comment.ui.*
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.collaboration.ui.codereview.diff.EditorComponentInlaysManager
import com.intellij.diff.tools.simple.SimpleOnesideDiffViewer
import com.intellij.diff.util.LineRange
import com.intellij.diff.util.Range
import com.intellij.openapi.editor.impl.EditorImpl

class GEPRSimpleOnesideDiffViewerReviewThreadsHandler(reviewProcessModel: GEPRReviewProcessModel,
                                                      commentableRangesModel: SingleValueModel<List<Range>?>,
                                                      reviewThreadsModel: SingleValueModel<List<GEPRDiffReviewThreadMapping>?>,
                                                      viewer: SimpleOnesideDiffViewer,
                                                      componentsFactory: GEPRDiffEditorReviewComponentsFactory,
                                                      cumulative: Boolean)
  : GEPRDiffViewerBaseReviewThreadsHandler<SimpleOnesideDiffViewer>(commentableRangesModel, reviewThreadsModel, viewer) {

  private val commentableRanges = SingleValueModel<List<LineRange>>(emptyList())
  private val editorThreads = GEPREditorReviewThreadsModel()

  override val viewerReady: Boolean = true

  init {
    val inlaysManager = EditorComponentInlaysManager(viewer.editor as EditorImpl)

    val gutterIconRendererFactory = GEPRDiffEditorGutterIconRendererFactoryImpl(reviewProcessModel,
                                                                                inlaysManager,
                                                                                componentsFactory,
                                                                                cumulative) { line ->
      val (startLine, endLine) = getCommentLinesRange(viewer.editor, line)
      GEPRCommentLocation(viewer.side, endLine, startLine)
    }

    GEPREditorCommentableRangesController(commentableRanges, gutterIconRendererFactory, viewer.editor)
    GEPREditorReviewThreadsController(editorThreads, componentsFactory, inlaysManager)
  }

  override fun markCommentableRanges(ranges: List<Range>?) {
    commentableRanges.value = ranges?.let { GEPRCommentsUtil.getLineRanges(it, viewer.side) }.orEmpty()
  }

  override fun showThreads(threads: List<GEPRDiffReviewThreadMapping>?) {
    editorThreads.update(threads
                           ?.filter { it.diffSide == viewer.side }
                           ?.groupBy({ it.fileLineIndex }, { it.thread }).orEmpty())
  }
}
