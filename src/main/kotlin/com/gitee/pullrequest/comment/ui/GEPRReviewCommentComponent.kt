// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.comment.ui

import com.gitee.api.data.pullrequest.GEPullRequestReviewCommentState
import com.gitee.i18n.GiteeBundle
import com.gitee.pullrequest.comment.GESuggestedChange
import com.gitee.pullrequest.data.provider.GEPRReviewDataProvider
import com.gitee.pullrequest.ui.GEEditableHtmlPaneHandle
import com.gitee.pullrequest.ui.GETextActions
import com.gitee.pullrequest.ui.changes.GEPRSuggestedChangeHelper
import com.gitee.ui.avatars.GEAvatarIconsProvider
import com.gitee.ui.util.GEUIUtil
import com.gitee.ui.util.HtmlEditorPane
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.HtmlBuilder
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import net.miginfocom.layout.AC
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import javax.swing.JComponent
import javax.swing.JPanel


object GEPRReviewCommentComponent {

  fun create(project: Project,
             thread: GEPRReviewThreadModel,
             comment: GEPRReviewCommentModel,
             reviewDataProvider: GEPRReviewDataProvider,
             avatarIconsProvider: GEAvatarIconsProvider,
             suggestedChangeHelper: GEPRSuggestedChangeHelper,
             showResolvedMarker: Boolean = true): JComponent {

    val avatarLabel = ActionLink("") {
      comment.authorLinkUrl?.let { BrowserUtil.browse(it) }
    }.apply {
      icon = avatarIconsProvider.getIcon(comment.authorAvatarUrl)
      putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, true)
    }

    val titlePane = HtmlEditorPane().apply {
      foreground = UIUtil.getContextHelpForeground()
      putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, true)
    }
    val pendingLabel = JBLabel(" ${GiteeBundle.message("pull.request.review.comment.pending")} ", UIUtil.ComponentStyle.SMALL).apply {
      foreground = UIUtil.getContextHelpForeground()
      background = JBUI.CurrentTheme.Validator.warningBackgroundColor()
    }.andOpaque()
    val resolvedLabel = JBLabel(" ${GiteeBundle.message("pull.request.review.comment.resolved")} ", UIUtil.ComponentStyle.SMALL).apply {
      foreground = UIUtil.getContextHelpForeground()
      background = UIUtil.getPanelBackground()
    }.andOpaque()

    val commentPanel = JPanel(VerticalLayout(8, VerticalLayout.FILL)).apply {
      putClientProperty(UIUtil.HIDE_EDITOR_FROM_DATA_CONTEXT_PROPERTY, true)
      isOpaque = false
    }

    Controller(project,
               thread, comment,
               suggestedChangeHelper,
               titlePane, pendingLabel, resolvedLabel, commentPanel,
               showResolvedMarker)

    val editablePaneHandle = GEEditableHtmlPaneHandle(project, commentPanel, comment::body) {
      reviewDataProvider.updateComment(EmptyProgressIndicator(), comment.id, it)
    }

    val editButton = GETextActions.createEditButton(editablePaneHandle).apply {
      isVisible = comment.canBeUpdated
    }
    val deleteButton = GETextActions.createDeleteButton {
      reviewDataProvider.deleteComment(EmptyProgressIndicator(), comment.id)
    }.apply {
      isVisible = comment.canBeDeleted
    }

    return JPanel(null).apply {
      isOpaque = false
      layout = MigLayout(LC().gridGap("0", "0")
                           .insets("0", "0", "0", "0")
                           .fill(),
                         AC().gap("${JBUIScale.scale(8)}"))

      add(avatarLabel, CC().pushY())
      add(titlePane, CC().minWidth("0").split(5).alignX("left").pushX())
      add(pendingLabel, CC().hideMode(3).alignX("left"))
      add(resolvedLabel, CC().hideMode(3).alignX("left"))
      add(editButton, CC().hideMode(3).gapBefore("${JBUIScale.scale(12)}"))
      add(deleteButton, CC().hideMode(3).gapBefore("${JBUIScale.scale(8)}"))
      add(editablePaneHandle.panel, CC().newline().skip().push().minWidth("0").minHeight("0").growX().maxWidth("${getMaxWidth()}"))
    }
  }

  private fun getMaxWidth() = GEUIUtil.getPRTimelineWidth() - JBUIScale.scale(GEUIUtil.AVATAR_SIZE) + AllIcons.Actions.Close.iconWidth

  private class Controller(private val project: Project,
                           private val thread: GEPRReviewThreadModel,
                           private val comment: GEPRReviewCommentModel,
                           private val suggestedChangeHelper: GEPRSuggestedChangeHelper,
                           private val titlePane: HtmlEditorPane,
                           private val pendingLabel: JComponent,
                           private val resolvedLabel: JComponent,
                           private val commentPanel: JComponent,
                           private val showResolvedMarker: Boolean) {
    init {
      comment.addChangesListener {
        update()
      }
      update()
    }

    private fun update() {
      val commentComponentFactory = GEPRReviewCommentComponentFactory(project)
      val commentComponent = if (GESuggestedChange.containsSuggestedChange(comment.body)) {
        val suggestedChange = GESuggestedChange.create(comment.body,
                                                       thread.diffHunk, thread.filePath,
                                                       thread.startLine ?: thread.line, thread.line)
        commentComponentFactory.createCommentWithSuggestedChangeComponent(thread, suggestedChange, suggestedChangeHelper)
      }
      else {
        commentComponentFactory.createCommentComponent(comment.body)
      }

      commentPanel.removeAll()
      commentPanel.add(commentComponent)

      val authorLink = HtmlBuilder()
        .appendLink(comment.authorLinkUrl.orEmpty(), comment.authorUsername ?: GiteeBundle.message("user.someone"))
        .toString()

      when (comment.state) {
        GEPullRequestReviewCommentState.PENDING -> {
          pendingLabel.isVisible = true
          titlePane.setBody(authorLink)
        }

        GEPullRequestReviewCommentState.SUBMITTED -> {
          pendingLabel.isVisible = false
          titlePane.setBody(GiteeBundle.message("pull.request.review.commented", authorLink,
                                                 GEUIUtil.formatActionDate(comment.dateCreated)))
        }
      }

      resolvedLabel.isVisible = comment.isFirstInResolvedThread && showResolvedMarker
    }
  }

  fun factory(project: Project,
              thread: GEPRReviewThreadModel,
              reviewDataProvider: GEPRReviewDataProvider,
              avatarIconsProvider: GEAvatarIconsProvider,
              suggestedChangeHelper: GEPRSuggestedChangeHelper,
              showResolvedMarkerOnFirstComment: Boolean = true)
    : (GEPRReviewCommentModel) -> JComponent {
    return { comment ->
      create(
        project,
        thread, comment,
        reviewDataProvider, avatarIconsProvider,
        suggestedChangeHelper,
        showResolvedMarkerOnFirstComment)
    }
  }
}
