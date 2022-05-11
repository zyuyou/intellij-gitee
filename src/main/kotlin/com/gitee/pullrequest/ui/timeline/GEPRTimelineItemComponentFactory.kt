// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.timeline

import com.gitee.api.data.*
import com.gitee.api.data.pullrequest.GEPullRequestCommitShort
import com.gitee.api.data.pullrequest.GEPullRequestReview
import com.gitee.api.data.pullrequest.GEPullRequestReviewState.*
import com.gitee.api.data.pullrequest.timeline.GEPRTimelineEvent
import com.gitee.api.data.pullrequest.timeline.GEPRTimelineItem
import com.gitee.i18n.GiteeBundle
import com.gitee.icons.GiteeIcons
import com.gitee.pullrequest.comment.convertToHtml
import com.gitee.pullrequest.comment.ui.GEPRReviewThreadComponent
import com.gitee.pullrequest.data.provider.GEPRCommentsDataProvider
import com.gitee.pullrequest.data.provider.GEPRDetailsDataProvider
import com.gitee.pullrequest.data.provider.GEPRReviewDataProvider
import com.gitee.pullrequest.ui.GEEditableHtmlPaneHandle
import com.gitee.pullrequest.ui.GETextActions
import com.gitee.pullrequest.ui.changes.GEPRSuggestedChangeHelper
import com.gitee.ui.avatars.GEAvatarIconsProvider
import com.gitee.ui.util.GEUIUtil
import com.gitee.ui.util.HtmlEditorPane
import com.intellij.collaboration.async.CompletableFutureUtil.successOnEdt
import com.intellij.collaboration.ui.codereview.timeline.TimelineItemComponentFactory
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.plugins.newui.HorizontalLayout
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.labels.LinkListener
import com.intellij.ui.components.panels.HorizontalBox
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import org.intellij.lang.annotations.Language
import java.awt.Dimension
import java.util.*
import javax.swing.*
import kotlin.math.ceil
import kotlin.math.floor

class GEPRTimelineItemComponentFactory(private val project: Project,
                                       private val detailsDataProvider: GEPRDetailsDataProvider,
                                       private val commentsDataProvider: GEPRCommentsDataProvider,
                                       private val reviewDataProvider: GEPRReviewDataProvider,
                                       private val avatarIconsProvider: GEAvatarIconsProvider,
                                       private val reviewsThreadsModelsProvider: GEPRReviewsThreadsModelsProvider,
                                       private val reviewDiffComponentFactory: GEPRReviewThreadDiffComponentFactory,
                                       private val eventComponentFactory: GEPRTimelineEventComponentFactory<GEPRTimelineEvent>,
                                       private val selectInToolWindowHelper: GEPRSelectInToolWindowHelper,
                                       private val suggestedChangeHelper: GEPRSuggestedChangeHelper,
                                       private val currentUser: GiteeUser
) : TimelineItemComponentFactory<GEPRTimelineItem> {

  override fun createComponent(item: GEPRTimelineItem): Item {
    try {
      return when (item) {
        is GEPullRequestCommitShort -> createComponent(item)

        is GEIssueComment -> createComponent(item)
        is GEPullRequestReview -> createComponent(item)

        is GEPRTimelineEvent -> eventComponentFactory.createComponent(item)
        is GEPRTimelineItem.Unknown -> throw IllegalStateException("Unknown item type: " + item.__typename)
        else -> error("Undefined item type")
      }
    }
    catch (e: Exception) {
      LOG.warn(e)
      return Item(AllIcons.General.Warning, HtmlEditorPane(GiteeBundle.message("cannot.display.item", e.message ?: "")))
    }
  }

  private fun createComponent(commit: GEPullRequestCommitShort): Item {
    val gitCommit = commit.commit
    val titlePanel = NonOpaquePanel(HorizontalLayout(JBUIScale.scale(8))).apply {
      add(userAvatar(gitCommit.author))
      add(HtmlEditorPane(gitCommit.messageHeadlineHTML))
      add(ActionLink(gitCommit.abbreviatedOid) {
        selectInToolWindowHelper.selectCommit(gitCommit.abbreviatedOid)
      })
    }

    return Item(AllIcons.Vcs.CommitNode, titlePanel)
  }

  fun createComponent(details: GiteePullRequest): Item {
    val contentPanel: JPanel?
    val actionsPanel: JPanel?

//    if (details is GiteePullRequest) {
//      val textPane = HtmlEditorPane(details.body.convertToHtml(project))
//      val panelHandle = GEEditableHtmlPaneHandle(project, textPane, details::body) { newText ->
//        detailsDataProvider.updateDetails(EmptyProgressIndicator(), description = newText)
//          .successOnEdt { textPane.setBody(it.body.convertToHtml(project)) }
//      }
//      contentPanel = panelHandle.panel
//      actionsPanel = if (details.viewerCanUpdate) NonOpaquePanel(HorizontalLayout(JBUIScale.scale(8))).apply {
//        add(GETextActions.createEditButton(panelHandle))
//      }
//      else null
//    }
//    else {
//      contentPanel = null
//      actionsPanel = null
//    }
    contentPanel = null
    actionsPanel = null

    val titlePanel = NonOpaquePanel(HorizontalLayout(JBUIScale.scale(12))).apply {
      add(actionTitle(details.author, GiteeBundle.message("pull.request.timeline.created"), details.createdAt))
//      if (actionsPanel != null && actionsPanel.componentCount > 0) add(actionsPanel)
    }

//    return Item(userAvatar(details.author), titlePanel, contentPanel)
    return Item(userAvatar(details.author), titlePanel, contentPanel)
  }

  private fun createComponent(comment: GEIssueComment): Item {
    val textPane = HtmlEditorPane(comment.body.convertToHtml(project))
    val panelHandle = GEEditableHtmlPaneHandle(project, textPane, comment::body) { newText ->
      commentsDataProvider.updateComment(EmptyProgressIndicator(), comment.id, newText)
        .successOnEdt { textPane.setBody(it.convertToHtml(project)) }
    }
    val actionsPanel = NonOpaquePanel(HorizontalLayout(JBUIScale.scale(8))).apply {
      if (comment.viewerCanUpdate) add(GETextActions.createEditButton(panelHandle))
      if (comment.viewerCanDelete) add(GETextActions.createDeleteButton {
        commentsDataProvider.deleteComment(EmptyProgressIndicator(), comment.id)
      })
    }
    val titlePanel = NonOpaquePanel(HorizontalLayout(JBUIScale.scale(12))).apply {
      add(actionTitle(comment.author, GiteeBundle.message("pull.request.timeline.commented"), comment.createdAt))
      if (actionsPanel.componentCount > 0) add(actionsPanel)
    }

    return Item(userAvatar(comment.author), titlePanel, panelHandle.panel)
  }

  private fun createComponent(review: GEPullRequestReview): Item {
    val reviewThreadsModel = reviewsThreadsModelsProvider.getReviewThreadsModel(review.id)
    val panelHandle: GEEditableHtmlPaneHandle?
    if (review.body.isNotEmpty()) {
      val textPane = HtmlEditorPane(review.body.convertToHtml(project))
      panelHandle =
        GEEditableHtmlPaneHandle(project, textPane, review::body, { newText ->
          reviewDataProvider.updateReviewBody(EmptyProgressIndicator(), review.id, newText)
            .successOnEdt { textPane.setBody(it.convertToHtml(project)) }
        })
    }
    else {
      panelHandle = null
    }

    val actionsPanel = NonOpaquePanel(HorizontalLayout(JBUIScale.scale(8))).apply {
      if (panelHandle != null && review.viewerCanUpdate) add(GETextActions.createEditButton(panelHandle))
    }

    val contentPanel = NonOpaquePanel(VerticalLayout(12)).apply {
      border = JBUI.Borders.emptyTop(4)
      if (panelHandle != null) add(panelHandle.panel)
      add(GEPRReviewThreadsPanel.create(reviewThreadsModel) {
        GEPRReviewThreadComponent.createWithDiff(project, it,
                                                 reviewDataProvider, avatarIconsProvider,
                                                 reviewDiffComponentFactory,
                                                 selectInToolWindowHelper, suggestedChangeHelper,
                                                 currentUser)
      })
    }
    val actionText = when (review.state) {
      APPROVED -> GiteeBundle.message("pull.request.timeline.approved.changes")
      CHANGES_REQUESTED -> GiteeBundle.message("pull.request.timeline.requested.changes")
      PENDING -> GiteeBundle.message("pull.request.timeline.started.review")
      COMMENTED, DISMISSED -> GiteeBundle.message("pull.request.timeline.reviewed")
    }
    val titlePanel = NonOpaquePanel(HorizontalLayout(JBUIScale.scale(12))).apply {
      add(actionTitle(avatarIconsProvider, review.author, actionText, review.createdAt))
      if (actionsPanel.componentCount > 0) add(actionsPanel)
    }

    val icon = when (review.state) {
      APPROVED -> GiteeIcons.ReviewAccepted
      CHANGES_REQUESTED -> GiteeIcons.ReviewRejected
      COMMENTED -> GiteeIcons.Review
      DISMISSED -> GiteeIcons.Review
      PENDING -> GiteeIcons.Review
    }

    return Item(icon, titlePanel, contentPanel, NOT_DEFINED_SIZE)
  }

  private fun userAvatar(user: GEActor?): JLabel {
    return userAvatar(avatarIconsProvider, user)
  }

  private fun userAvatar(user: GEGitActor?): JLabel {
    return LinkLabel<Any>("", avatarIconsProvider.getIcon(user?.avatarUrl), LinkListener { _, _ ->
      user?.url?.let { BrowserUtil.browse(it) }
    })
  }

  class Item(val marker: JLabel, title: JComponent, content: JComponent? = null, size: Dimension = getDefaultSize()) : JPanel() {

    constructor(markerIcon: Icon, title: JComponent, content: JComponent? = null, size: Dimension = getDefaultSize())
      : this(createMarkerLabel(markerIcon), title, content, size)

    init {
      isOpaque = false
      layout = MigLayout(LC().gridGap("0", "0")
                           .insets("0", "0", "0", "0")
                           .fill()).apply {
        columnConstraints = "[]${JBUIScale.scale(8)}[]"
      }

      add(marker, CC().pushY())
      add(title, CC().pushX())
      if (content != null) add(content, CC().newline().skip().grow().push().maxWidth(size))
    }

    companion object {
      private fun CC.maxWidth(dimension: Dimension) = if (dimension.width > 0) this.maxWidth("${dimension.width}") else this

      private fun createMarkerLabel(markerIcon: Icon) =
        JLabel(markerIcon).apply {
          val verticalGap = if (markerIcon.iconHeight < 20) (20f - markerIcon.iconHeight) / 2 else 0f
          val horizontalGap = if (markerIcon.iconWidth < 20) (20f - markerIcon.iconWidth) / 2 else 0f
          border = JBUI.Borders.empty(floor(verticalGap).toInt(), floor(horizontalGap).toInt(),
                                      ceil(verticalGap).toInt(), ceil(horizontalGap).toInt())
        }
    }
  }

  companion object {
    private val LOG = logger<GEPRTimelineItemComponentFactory>()
    private val NOT_DEFINED_SIZE = Dimension(-1, -1)

    fun getDefaultSize() = Dimension(GEUIUtil.getPRTimelineWidth(), -1)

    fun userAvatar(avatarIconsProvider: GEAvatarIconsProvider, user: GEActor?): JLabel {
      return LinkLabel<Any>("", avatarIconsProvider.getIcon(user?.avatarUrl), LinkListener { _, _ ->
        user?.url?.let { BrowserUtil.browse(it) }
      })
    }

    fun actionTitle(avatarIconsProvider: GEAvatarIconsProvider, actor: GEActor?, @Language("HTML") actionHTML: String, date: Date)
      : JComponent {
      return HorizontalBox().apply {
        add(userAvatar(avatarIconsProvider, actor))
        add(Box.createRigidArea(JBDimension(8, 0)))
        add(actionTitle(actor, actionHTML, date))
      }
    }

    fun actionTitle(actor: GEActor?, actionHTML: String, date: Date): JComponent {
      //language=HTML
      val text = """<a href='${actor?.url}'>${actor?.login ?: "unknown"}</a> $actionHTML ${GEUIUtil.formatActionDate(date)}"""

      return HtmlEditorPane(text).apply {
        foreground = UIUtil.getContextHelpForeground()
      }
    }
  }
}