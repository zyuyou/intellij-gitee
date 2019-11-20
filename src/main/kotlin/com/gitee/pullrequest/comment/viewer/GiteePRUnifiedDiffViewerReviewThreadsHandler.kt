// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.comment.viewer

import com.gitee.pullrequest.comment.ui.EditorComponentInlaysManager
import com.gitee.pullrequest.comment.ui.GiteePREditorReviewThreadComponentFactory
import com.gitee.pullrequest.comment.ui.GiteePREditorReviewThreadsController
import com.gitee.pullrequest.comment.ui.GiteePREditorReviewThreadsModel
import com.gitee.pullrequest.data.model.GiteePRDiffReviewThreadMapping
import com.intellij.diff.tools.fragmented.UnifiedDiffViewer
import com.intellij.diff.tools.util.base.DiffViewerListener
import com.intellij.openapi.editor.impl.EditorImpl

class GiteePRUnifiedDiffViewerReviewThreadsHandler(viewer: UnifiedDiffViewer,
                                                   componentFactory: GiteePREditorReviewThreadComponentFactory)
  : GiteePRDiffViewerBaseReviewThreadsHandler<UnifiedDiffViewer>(viewer, componentFactory) {

  private val editorThreads = GiteePREditorReviewThreadsModel()

  override val viewerReady: Boolean
    get() = viewer.isContentGood

  init {
    val inlaysManager = EditorComponentInlaysManager(viewer.editor as EditorImpl)
    GiteePREditorReviewThreadsController(editorThreads, componentFactory, inlaysManager)

    viewer.addListener(object : DiffViewerListener() {
      override fun onAfterRediff() {
        updateThreads(mappings)
      }
    })
  }

  override fun updateThreads(mappings: List<GiteePRDiffReviewThreadMapping>) {
    editorThreads.update(mappings.groupBy { viewer.transferLineToOneside(it.side, it.fileLineIndex) }.filter { (line, _) -> line >= 0 })
  }
}




