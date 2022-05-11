// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.comment

import com.gitee.api.data.GiteeUser
import com.gitee.api.data.pullrequest.GEPullRequestPendingReview
import com.gitee.api.data.pullrequest.GEPullRequestReviewThread
import com.gitee.pullrequest.comment.ui.GEPRDiffEditorReviewComponentsFactoryImpl
import com.gitee.pullrequest.comment.ui.GEPRReviewProcessModelImpl
import com.gitee.pullrequest.comment.viewer.GEPRSimpleOnesideDiffViewerReviewThreadsHandler
import com.gitee.pullrequest.comment.viewer.GEPRTwosideDiffViewerReviewThreadsHandler
import com.gitee.pullrequest.comment.viewer.GEPRUnifiedDiffViewerReviewThreadsHandler
import com.gitee.pullrequest.data.GEPRChangeDiffData
import com.gitee.pullrequest.data.provider.GEPRDetailsDataProvider
import com.gitee.pullrequest.data.provider.GEPRReviewDataProvider
import com.gitee.pullrequest.data.service.GEPRRepositoryDataService
import com.gitee.pullrequest.ui.GECompletableFutureLoadingModel
import com.gitee.pullrequest.ui.GELoadingModel
import com.gitee.pullrequest.ui.GESimpleLoadingModel
import com.gitee.pullrequest.ui.changes.GEPRCreateDiffCommentParametersHelper
import com.gitee.pullrequest.ui.changes.GEPRSuggestedChangeHelper
import com.gitee.ui.avatars.GEAvatarIconsProvider
import com.gitee.util.GEPatchHunkUtil
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.diff.tools.fragmented.UnifiedDiffViewer
import com.intellij.diff.tools.simple.SimpleOnesideDiffViewer
import com.intellij.diff.tools.util.base.DiffViewerBase
import com.intellij.diff.tools.util.side.TwosideTextDiffViewer
import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.Disposable
import com.intellij.openapi.diff.impl.patch.PatchReader
import com.intellij.openapi.project.Project
import java.util.function.Function
import kotlin.properties.Delegates.observable

class GEPRDiffReviewSupportImpl(private val project: Project,
                                private val reviewDataProvider: GEPRReviewDataProvider,
                                private val detailsDataProvider: GEPRDetailsDataProvider,
                                private val avatarIconsProvider: GEAvatarIconsProvider,
                                private val repositoryDataService: GEPRRepositoryDataService,
                                private val diffData: GEPRChangeDiffData,
                                private val currentUser: GiteeUser
)
  : GEPRDiffReviewSupport {

  private var pendingReviewLoadingModel: GESimpleLoadingModel<GEPullRequestPendingReview?>? = null
  private val reviewProcessModel = GEPRReviewProcessModelImpl()

  private var reviewThreadsLoadingModel: GESimpleLoadingModel<List<GEPRDiffReviewThreadMapping>>? = null
  private val reviewThreadsModel = SingleValueModel<List<GEPRDiffReviewThreadMapping>?>(null)

  override val isLoadingReviewData: Boolean
    get() = reviewThreadsLoadingModel?.loading == true || pendingReviewLoadingModel?.loading == true

  override var showReviewThreads by observable(true) { _, _, _ ->
    updateReviewThreads()
  }

  override var showResolvedReviewThreads by observable(false) { _, _, _ ->
    updateReviewThreads()
  }

  override fun install(viewer: DiffViewerBase) {
    val diffRangesModel = SingleValueModel(if (reviewDataProvider.canComment()) diffData.diffRanges else null)

    if (reviewDataProvider.canComment()) {
      loadPendingReview(viewer)
      var rangesInstalled = false
      reviewProcessModel.addAndInvokeChangesListener {
        if (reviewProcessModel.isActual && !rangesInstalled) {
          diffRangesModel.value = diffData.diffRanges
          rangesInstalled = true
        }
      }
    }

    loadReviewThreads(viewer)

    val createCommentParametersHelper = GEPRCreateDiffCommentParametersHelper(diffData.commitSha, diffData.filePath, diffData.linesMapper)
    val suggestedChangesHelper = GEPRSuggestedChangeHelper(project,
                                                           viewer, repositoryDataService.remoteCoordinates.repository,
                                                           reviewDataProvider,
                                                           detailsDataProvider)
    val componentsFactory = GEPRDiffEditorReviewComponentsFactoryImpl(project,
                                                                      reviewDataProvider, avatarIconsProvider,
                                                                      createCommentParametersHelper, suggestedChangesHelper,
                                                                      currentUser)
    val cumulative = diffData is GEPRChangeDiffData.Cumulative
    when (viewer) {
      is SimpleOnesideDiffViewer ->
        GEPRSimpleOnesideDiffViewerReviewThreadsHandler(reviewProcessModel, diffRangesModel, reviewThreadsModel, viewer, componentsFactory,
                                                        cumulative)
      is UnifiedDiffViewer ->
        GEPRUnifiedDiffViewerReviewThreadsHandler(reviewProcessModel, diffRangesModel, reviewThreadsModel, viewer, componentsFactory,
                                                  cumulative)
      is TwosideTextDiffViewer ->
        GEPRTwosideDiffViewerReviewThreadsHandler(reviewProcessModel, diffRangesModel, reviewThreadsModel, viewer, componentsFactory,
                                                  cumulative)
      else -> return
    }
  }

  override fun reloadReviewData() {
    reviewDataProvider.resetPendingReview()
    reviewDataProvider.resetReviewThreads()
  }

  private fun loadPendingReview(disposable: Disposable) {
    val loadingModel = GECompletableFutureLoadingModel<GEPullRequestPendingReview?>(disposable).also {
      it.addStateChangeListener(object : GELoadingModel.StateChangeListener {
        override fun onLoadingCompleted() {
          if (it.resultAvailable) {
            reviewProcessModel.populatePendingReviewData(it.result)
          }
        }
      })
    }
    pendingReviewLoadingModel = loadingModel

    doLoadPendingReview(loadingModel)
    reviewDataProvider.addPendingReviewListener(disposable) {
      reviewProcessModel.clearPendingReviewData()
      doLoadPendingReview(loadingModel)
    }
  }

  private fun doLoadPendingReview(model: GECompletableFutureLoadingModel<GEPullRequestPendingReview?>) {
    model.future = reviewDataProvider.loadPendingReview()
  }

  private fun loadReviewThreads(disposable: Disposable) {
    val loadingModel = GECompletableFutureLoadingModel<List<GEPRDiffReviewThreadMapping>>(disposable).apply {
      addStateChangeListener(object : GELoadingModel.StateChangeListener {
        override fun onLoadingCompleted() = updateReviewThreads()
      })
    }
    reviewThreadsLoadingModel = loadingModel

    doLoadReviewThreads(loadingModel)
    reviewDataProvider.addReviewThreadsListener(disposable) {
      doLoadReviewThreads(loadingModel)
    }
  }

  private fun doLoadReviewThreads(model: GECompletableFutureLoadingModel<List<GEPRDiffReviewThreadMapping>>) {
    model.future = reviewDataProvider.loadReviewThreads().thenApplyAsync(Function {
      it.mapNotNull(::mapThread)
    }, ProcessIOExecutorService.INSTANCE)
  }

  private fun mapThread(thread: GEPullRequestReviewThread): GEPRDiffReviewThreadMapping? {
    val originalCommitSha = thread.originalCommit?.oid ?: return null
    if (!diffData.contains(originalCommitSha, thread.path)) return null

    val (side, line) = when (diffData) {
      is GEPRChangeDiffData.Cumulative -> thread.side to thread.line - 1
      is GEPRChangeDiffData.Commit -> {
        val patchReader = PatchReader(GEPatchHunkUtil.createPatchFromHunk(thread.path, thread.diffHunk))
        patchReader.readTextPatches()
        val patchHunk = patchReader.textPatches[0].hunks.lastOrNull() ?: return null
        val position = GEPatchHunkUtil.getHunkLinesCount(patchHunk) - 1
        val (unmappedSide, unmappedLine) = GEPatchHunkUtil.findSideFileLineFromHunkLineIndex(patchHunk, position) ?: return null
        diffData.mapPosition(originalCommitSha, unmappedSide, unmappedLine) ?: return null
      }
    }

    return GEPRDiffReviewThreadMapping(side, line, thread)
  }

  private fun updateReviewThreads() {
    val loadingModel = reviewThreadsLoadingModel ?: return
    if (loadingModel.loading) return
    reviewThreadsModel.value = if (showReviewThreads) loadingModel.result?.filter { showResolvedReviewThreads || !it.thread.isResolved } else null
  }
}
