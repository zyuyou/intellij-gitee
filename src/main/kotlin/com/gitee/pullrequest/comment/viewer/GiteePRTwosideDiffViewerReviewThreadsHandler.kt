// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.comment.viewer

import com.gitee.pullrequest.comment.ui.EditorComponentInlaysManager
import com.gitee.pullrequest.comment.ui.GiteePREditorReviewThreadComponentFactory
import com.gitee.pullrequest.comment.ui.GiteePREditorReviewThreadsController
import com.gitee.pullrequest.comment.ui.GiteePREditorReviewThreadsModel
import com.gitee.pullrequest.data.model.GiteePRDiffReviewThreadMapping
import com.intellij.diff.tools.util.side.TwosideTextDiffViewer
import com.intellij.diff.util.Side
import com.intellij.openapi.editor.impl.EditorImpl

class GiteePRTwosideDiffViewerReviewThreadsHandler(viewer: TwosideTextDiffViewer,
                                                   componentFactory: GiteePREditorReviewThreadComponentFactory)
  : GiteePRDiffViewerBaseReviewThreadsHandler<TwosideTextDiffViewer>(viewer, componentFactory) {

  private val editorsThreads: Map<Side, GiteePREditorReviewThreadsModel>

  override val viewerReady = true

  init {
    val editorThreadsLeft = GiteePREditorReviewThreadsModel()
    GiteePREditorReviewThreadsController(editorThreadsLeft, componentFactory,
                                      EditorComponentInlaysManager(viewer.editor1 as EditorImpl))

    val editorThreadsRight = GiteePREditorReviewThreadsModel()
    GiteePREditorReviewThreadsController(editorThreadsRight, componentFactory,
                                      EditorComponentInlaysManager(viewer.editor2 as EditorImpl))

    editorsThreads = mapOf(Side.LEFT to editorThreadsLeft, Side.RIGHT to editorThreadsRight)
  }

  override fun updateThreads(mappings: List<GiteePRDiffReviewThreadMapping>) {
    mappings.groupBy { it.side }.forEach { (side, mappings) ->
      editorsThreads[side]?.update(mappings.groupBy { it.fileLineIndex })
    }
  }
}

