// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.comment.viewer

import com.gitee.pullrequest.comment.ui.EditorComponentInlaysManager
import com.gitee.pullrequest.comment.ui.GiteePREditorReviewThreadComponentFactory
import com.gitee.pullrequest.comment.ui.GiteePREditorReviewThreadsController
import com.gitee.pullrequest.comment.ui.GiteePREditorReviewThreadsModel
import com.gitee.pullrequest.data.model.GiteePRDiffReviewThreadMapping
import com.intellij.diff.tools.simple.SimpleOnesideDiffViewer
import com.intellij.openapi.editor.impl.EditorImpl

class GiteePRSimpleOnesideDiffViewerReviewThreadsHandler(viewer: SimpleOnesideDiffViewer,
                                                         componentFactory: GiteePREditorReviewThreadComponentFactory)
  : GiteePRDiffViewerBaseReviewThreadsHandler<SimpleOnesideDiffViewer>(viewer, componentFactory) {

  private val editorThreads = GiteePREditorReviewThreadsModel()

  override val viewerReady: Boolean = true

  init {
    val inlaysManager = EditorComponentInlaysManager(viewer.editor as EditorImpl)
    GiteePREditorReviewThreadsController(editorThreads, componentFactory, inlaysManager)
  }

  override fun updateThreads(mappings: List<GiteePRDiffReviewThreadMapping>) {
    editorThreads.update(mappings.filter { it.side == viewer.side }.groupBy { it.fileLineIndex })
  }
}
