// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.timeline

import com.gitee.api.data.GEActor
import com.gitee.api.data.GECommit
import com.gitee.api.data.GEGitActor
import com.gitee.api.data.GEIssueComment
import com.gitee.api.data.pullrequest.GEPullRequestCommit
import com.gitee.api.data.pullrequest.GEPullRequestReview
import com.gitee.api.data.pullrequest.GiteePullRequestReviewState.*
import com.gitee.api.data.pullrequest.timeline.GiteePRTimelineEvent
import com.gitee.api.data.pullrequest.timeline.GiteePRTimelineItem
import com.gitee.icons.GiteeIcons
import com.gitee.pullrequest.avatars.GiteeAvatarIconsProvider
import com.gitee.ui.util.HtmlEditorPane
import com.gitee.util.GiteeUIUtil
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.labels.LinkListener
import com.intellij.ui.components.panels.HorizontalBox
import com.intellij.ui.components.panels.VerticalBox
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UI
import com.intellij.util.ui.UIUtil
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import org.intellij.lang.annotations.Language
import java.util.*
import javax.swing.*
import kotlin.math.ceil
import kotlin.math.floor

class GiteePRTimelineItemComponentFactory(private val avatarIconsProvider: GiteeAvatarIconsProvider,
                                       private val reviewsThreadsProvider: GiteePRReviewsThreadsProvider,
                                       private val reviewDiffComponentFactory: GiteePRReviewThreadDiffComponentFactory,
                                       private val eventComponentFactory: GiteePRTimelineEventComponentFactory<GiteePRTimelineEvent>) {

  fun createComponent(item: GiteePRTimelineItem): Item {
    try {
      return when (item) {
        is GEPullRequestCommit -> Item(AllIcons.Vcs.CommitNode, commitTitle(item.commit))

        is GEIssueComment -> createComponent(item)
        is GEPullRequestReview -> createComponent(item)

        is GiteePRTimelineEvent -> eventComponentFactory.createComponent(item)
        else -> throw IllegalStateException("Unknown item type")
      }
    }
    catch (e: Exception) {
      return Item(AllIcons.General.Warning, HtmlEditorPane("Cannot display item - ${e.message}"))
    }
  }

  private fun createComponent(model: GEIssueComment) =
    Item(userAvatar(model.author),
      actionTitle(model.author, "commented", model.createdAt),
      HtmlEditorPane(model.bodyHtml))

  private fun createComponent(review: GEPullRequestReview): Item {
    val threads = reviewsThreadsProvider.findReviewThreads(review.id) ?: throw IllegalStateException("Can't find threads")

    val reviewPanel = VerticalBox().apply {
      if (review.bodyHTML.isNotEmpty()) {
        add(HtmlEditorPane(review.bodyHTML).apply {
          border = JBUI.Borders.empty(4, 0)
        })
      }
      add(Box.createRigidArea(JBDimension(0, 6)))
      add(GiteePRReviewThreadsPanel(threads, avatarIconsProvider, reviewDiffComponentFactory).apply {
        border = JBUI.Borders.empty(2, 0)
      })
    }

    val icon = when (review.state) {
      APPROVED -> GiteeIcons.ReviewAccepted
      CHANGES_REQUESTED -> GiteeIcons.ReviewRejected
      COMMENTED -> GiteeIcons.Review
      DISMISSED -> GiteeIcons.Review
      PENDING -> GiteeIcons.Review
    }

    val actionText = when (review.state) {
      APPROVED -> "approved these changes"
      CHANGES_REQUESTED -> "rejected these changes"
      COMMENTED, DISMISSED, PENDING -> "reviewed"
    }

    return Item(icon, actionTitle(avatarIconsProvider, review.author, actionText, review.createdAt), reviewPanel)
  }

  private fun userAvatar(user: GEActor?): JLabel {
    return userAvatar(avatarIconsProvider, user)
  }

  private fun userAvatar(user: GEGitActor?): JLabel {
    return LinkLabel<Any>("", avatarIconsProvider.getIcon(user?.avatarUrl), LinkListener { _, _ ->
      user?.url?.let { BrowserUtil.browse(it) }
    })
  }

  private fun commitTitle(commit: GECommit): JComponent {
    //language=HTML
    val text = """${commit.messageHeadlineHTML} <a href='${commit.url}'>${commit.abbreviatedOid}</a>"""

    return HorizontalBox().apply {
      add(userAvatar(commit.author))
      add(Box.createRigidArea(JBDimension(8, 0)))
      add(HtmlEditorPane(text))
    }
  }

  class Item(val marker: JLabel, title: JComponent, content: JComponent? = null) : JPanel() {

    constructor(markerIcon: Icon, title: JComponent, content: JComponent? = null)
      : this(createMarkerLabel(markerIcon), title, content)

    init {
      isOpaque = false
      border = JBUI.Borders.empty(10, 0, 10, 0)
      layout = MigLayout(LC().gridGap("0", "0")
        .insets("0", "0", "0", "0")
        .fill()).apply {
        columnConstraints = "[]${UI.scale(8)}[]"
      }

      add(marker, CC().pushY())
      add(title, CC().growX().pushX())
      if (content != null) add(content, CC().newline().skip().grow().push())
    }

    companion object {
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
    fun userAvatar(avatarIconsProvider: GiteeAvatarIconsProvider, user: GEActor?): JLabel {
      return LinkLabel<Any>("", avatarIconsProvider.getIcon(user?.avatarUrl), LinkListener { _, _ ->
        user?.url?.let { BrowserUtil.browse(it) }
      })
    }

    fun actionTitle(avatarIconsProvider: GiteeAvatarIconsProvider, actor: GEActor?, @Language("HTML") actionHTML: String, date: Date)
      : JComponent {
      return HorizontalBox().apply {
        add(userAvatar(avatarIconsProvider, actor))
        add(Box.createRigidArea(JBDimension(8, 0)))
        add(actionTitle(actor, actionHTML, date))
      }
    }

    fun actionTitle(actor: GEActor?, actionHTML: String, date: Date): JComponent {
      //language=HTML
      val text = """<a href='${actor?.url}'>${actor?.login ?: "unknown"}</a> $actionHTML ${GiteeUIUtil.formatActionDate(date)}"""

      return HtmlEditorPane(text).apply {
        foreground = UIUtil.getContextHelpForeground()
      }
    }
  }
}
