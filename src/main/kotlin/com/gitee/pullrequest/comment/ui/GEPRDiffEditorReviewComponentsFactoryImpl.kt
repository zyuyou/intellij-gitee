// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.comment.ui

import com.gitee.api.data.GEPullRequestReviewEvent
import com.gitee.api.data.GiteeUser
import com.gitee.api.data.request.GEPullRequestDraftReviewComment
import com.gitee.api.data.request.GEPullRequestDraftReviewThread
import com.gitee.i18n.GiteeBundle
import com.gitee.pullrequest.data.provider.GEPRReviewDataProvider
import com.gitee.pullrequest.ui.changes.GEPRCreateDiffCommentParametersHelper
import com.gitee.pullrequest.ui.changes.GEPRSuggestedChangeHelper
import com.gitee.ui.avatars.GEAvatarIconsProvider
import com.intellij.collaboration.async.CompletableFutureUtil.successOnEdt
import com.intellij.collaboration.ui.codereview.comment.ReviewUIUtil
import com.intellij.diff.util.Side
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsActions
import com.intellij.util.ui.JBUI
import javax.swing.JComponent

class GEPRDiffEditorReviewComponentsFactoryImpl
internal constructor(private val project: Project,
                     private val reviewDataProvider: GEPRReviewDataProvider,
                     private val avatarIconsProvider: GEAvatarIconsProvider,
                     private val createCommentParametersHelper: GEPRCreateDiffCommentParametersHelper,
                     private val suggestedChangeHelper: GEPRSuggestedChangeHelper,
                     private val currentUser: GiteeUser
)
  : GEPRDiffEditorReviewComponentsFactory {

  override fun createThreadComponent(thread: GEPRReviewThreadModel): JComponent =
    GEPRReviewThreadComponent.create(project, thread,
                                     reviewDataProvider, avatarIconsProvider,
                                     suggestedChangeHelper,
                                     currentUser).apply {
      border = JBUI.Borders.empty(8, 8)
    }.let { ReviewUIUtil.createEditorInlayPanel(it) }

  override fun createSingleCommentComponent(side: Side, line: Int, startLine: Int, hideCallback: () -> Unit): JComponent {
    val textFieldModel = GECommentTextFieldModel(project) {
      val filePath = createCommentParametersHelper.filePath
      if (line == startLine) {
        val commitSha = createCommentParametersHelper.commitSha
        val diffLine = createCommentParametersHelper.findPosition(side, line) ?: error("Can't determine comment position")
        reviewDataProvider.createReview(EmptyProgressIndicator(), GEPullRequestReviewEvent.COMMENT, null, commitSha,
                                        listOf(GEPullRequestDraftReviewComment(it, filePath, diffLine))).successOnEdt {
          hideCallback()
        }
      }
      else {
        reviewDataProvider.createThread(EmptyProgressIndicator(), null, it, line + 1, side, startLine + 1, filePath).successOnEdt {
          hideCallback()
        }
      }
    }

    return createCommentComponent(textFieldModel, GiteeBundle.message("pull.request.diff.editor.review.comment"), hideCallback)
  }

  override fun createNewReviewCommentComponent(side: Side, line: Int, startLine: Int, hideCallback: () -> Unit): JComponent {
    val textFieldModel = GECommentTextFieldModel(project) {
      val filePath = createCommentParametersHelper.filePath
      val commitSha = createCommentParametersHelper.commitSha
      if (line == startLine) {
        val diffLine = createCommentParametersHelper.findPosition(side, line) ?: error("Can't determine comment position")
        reviewDataProvider.createReview(EmptyProgressIndicator(), null, null, commitSha,
                                        listOf(GEPullRequestDraftReviewComment(it, filePath, diffLine))).successOnEdt {
          hideCallback()
        }
      }
      else {
        reviewDataProvider.createReview(EmptyProgressIndicator(), null, null, commitSha, null,
                                        listOf(GEPullRequestDraftReviewThread(it, line + 1, filePath, side, startLine + 1, side)))
          .successOnEdt {
            hideCallback()
          }
      }
    }

    return createCommentComponent(textFieldModel, GiteeBundle.message("pull.request.diff.editor.review.start"), hideCallback)
  }

  override fun createReviewCommentComponent(reviewId: String, side: Side, line: Int, startLine: Int, hideCallback: () -> Unit): JComponent {
    val textFieldModel = GECommentTextFieldModel(project) {
      val filePath = createCommentParametersHelper.filePath
      if (line == startLine) {
        val commitSha = createCommentParametersHelper.commitSha
        val diffLine = createCommentParametersHelper.findPosition(side, line) ?: error("Can't determine comment position")
        reviewDataProvider.addComment(EmptyProgressIndicator(), reviewId, it, commitSha, filePath, diffLine).successOnEdt {
          hideCallback()
        }
      }
      else {
        reviewDataProvider.createThread(EmptyProgressIndicator(), reviewId, it, line + 1, side, startLine + 1, filePath).successOnEdt {
          hideCallback()
        }
      }
    }

    return createCommentComponent(textFieldModel, GiteeBundle.message("pull.request.diff.editor.review.comment"), hideCallback)
  }

  private fun createCommentComponent(
    textFieldModel: GECommentTextFieldModel,
    @NlsActions.ActionText actionName: String,
    hideCallback: () -> Unit
  ): JComponent =
    GECommentTextFieldFactory(textFieldModel).create(avatarIconsProvider, currentUser, actionName) {
      hideCallback()
    }.apply {
      border = JBUI.Borders.empty(8)
    }.let { ReviewUIUtil.createEditorInlayPanel(it) }
}