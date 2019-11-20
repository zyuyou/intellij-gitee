// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.comment

import com.gitee.pullrequest.comment.ui.GiteePREditorReviewThreadComponentFactory
import com.gitee.pullrequest.comment.viewer.GiteePRDiffViewerBaseReviewThreadsHandler
import com.gitee.pullrequest.comment.viewer.GiteePRSimpleOnesideDiffViewerReviewThreadsHandler
import com.gitee.pullrequest.comment.viewer.GiteePRTwosideDiffViewerReviewThreadsHandler
import com.gitee.pullrequest.comment.viewer.GiteePRUnifiedDiffViewerReviewThreadsHandler
import com.gitee.pullrequest.data.GiteePullRequestDataProvider
import com.gitee.util.handleOnEdt
import com.intellij.diff.tools.fragmented.UnifiedDiffViewer
import com.intellij.diff.tools.simple.SimpleOnesideDiffViewer
import com.intellij.diff.tools.util.base.DiffViewerBase
import com.intellij.diff.tools.util.base.ListenerDiffViewerBase
import com.intellij.diff.tools.util.side.TwosideTextDiffViewer
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.changes.Change

class GiteePRDiffReviewThreadsProviderImpl(private val dataProvider: GiteePullRequestDataProvider,
                                           private val componentFactory: GiteePREditorReviewThreadComponentFactory)
  : GiteePRDiffReviewThreadsProvider {

  override fun install(viewer: DiffViewerBase, change: Change) {
    val commentsHandler = when (viewer) {
      is SimpleOnesideDiffViewer ->
        GiteePRSimpleOnesideDiffViewerReviewThreadsHandler(viewer, componentFactory)
      is UnifiedDiffViewer ->
        GiteePRUnifiedDiffViewerReviewThreadsHandler(viewer, componentFactory)
      is TwosideTextDiffViewer ->
        GiteePRTwosideDiffViewerReviewThreadsHandler(viewer, componentFactory)
      else -> return
    }
    Disposer.register(viewer, commentsHandler)

    loadAndShowComments(commentsHandler, change)
    dataProvider.addRequestsChangesListener(commentsHandler, object : GiteePullRequestDataProvider.RequestsChangedListener {
      override fun reviewThreadsRequestChanged() {
        loadAndShowComments(commentsHandler, change)
      }
    })
  }

  private fun loadAndShowComments(commentsHandler: GiteePRDiffViewerBaseReviewThreadsHandler<out ListenerDiffViewerBase>,
                                  change: Change) {
    val disposable = Disposer.newDisposable()
    dataProvider.filesReviewThreadsRequest.handleOnEdt(disposable) { result, error ->
      if (result != null) {
        commentsHandler.mappings = result[change].orEmpty()
      }
      if (error != null) {
        LOG.info("Failed to load and process file comments", error)
      }
    }
    Disposer.register(commentsHandler, disposable)
  }

  companion object {
    val LOG = logger<GiteePRDiffReviewThreadsProviderImpl>()
  }
}