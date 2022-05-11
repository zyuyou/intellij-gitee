// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.timeline

import com.gitee.api.data.GEUser
import com.gitee.api.data.GiteeIssueLabel
import com.gitee.api.data.pullrequest.GEGitRefName
import com.gitee.api.data.pullrequest.GEPullRequestRequestedReviewer
import com.gitee.api.data.pullrequest.GEPullRequestState
import com.gitee.api.data.pullrequest.timeline.*
import com.gitee.i18n.GiteeBundle
import com.gitee.icons.GiteeIcons
import com.gitee.pullrequest.ui.timeline.GEPRTimelineItemComponentFactory.Item
import com.gitee.ui.avatars.GEAvatarIconsProvider
import com.gitee.ui.util.GEUIUtil
import com.gitee.ui.util.HtmlEditorPane
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vcs.changes.ui.CurrentBranchComponent
import com.intellij.ui.ColorUtil
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import icons.CollaborationToolsIcons
import org.intellij.lang.annotations.Language
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel

class GEPRTimelineEventComponentFactoryImpl(private val avatarIconsProvider: GEAvatarIconsProvider)
  : GEPRTimelineEventComponentFactory<GEPRTimelineEvent> {

  private val simpleEventDelegate = SimpleEventComponentFactory()
  private val stateEventDelegate = StateEventComponentFactory()
  private val branchEventDelegate = BranchEventComponentFactory()
  private val complexEventDelegate = ComplexEventComponentFactory()

  override fun createComponent(event: GEPRTimelineEvent): Item {
    return when (event) {
      is GEPRTimelineEvent.Simple -> simpleEventDelegate.createComponent(event)
      is GEPRTimelineEvent.State -> stateEventDelegate.createComponent(event)
      is GEPRTimelineEvent.Branch -> branchEventDelegate.createComponent(event)
      is GEPRTimelineEvent.Complex -> complexEventDelegate.createComponent(event)
      else -> throwUnknownType(event)
    }
  }

  private fun throwUnknownType(item: GEPRTimelineEvent): Nothing {
    throw IllegalStateException("""Unknown event type "${item.javaClass.canonicalName}"""")
  }

  private abstract inner class EventComponentFactory<T : GEPRTimelineEvent> : GEPRTimelineEventComponentFactory<T> {

    protected fun eventItem(item: GEPRTimelineEvent, @Language("HTML") titleHTML: String): Item {
      return eventItem(GiteeIcons.Timeline, item, titleHTML)
    }

    protected fun eventItem(markerIcon: Icon,
                            item: GEPRTimelineEvent,
                            @Language("HTML") titleHTML: String): Item {
      return Item(markerIcon, GEPRTimelineItemComponentFactory.actionTitle(avatarIconsProvider, item.actor, titleHTML, item.createdAt))
    }
  }

  private inner class SimpleEventComponentFactory : EventComponentFactory<GEPRTimelineEvent.Simple>() {
    override fun createComponent(event: GEPRTimelineEvent.Simple): Item {
      return when (event) {
        is GEPRAssignedEvent ->
          eventItem(event, assigneesHTML(assigned = listOf(event.user)))
        is GEPRUnassignedEvent ->
          eventItem(event, assigneesHTML(unassigned = listOf(event.user)))

        is GEPRReviewRequestedEvent ->
          eventItem(event, reviewersHTML(added = listOf(event.requestedReviewer)))
        is GEPRReviewUnrequestedEvent ->
          eventItem(event, reviewersHTML(removed = listOf(event.requestedReviewer)))

        is GEPRLabeledEvent ->
          eventItem(event, labelsHTML(added = listOf(event.label)))
        is GEPRUnlabeledEvent ->
          eventItem(event, labelsHTML(removed = listOf(event.label)))

        is GEPRRenamedTitleEvent ->
          eventItem(event, renameHTML(event.previousTitle, event.currentTitle))

        is GEPRTimelineMergedSimpleEvents -> {
          val builder = StringBuilder()
            .appendParagraph(labelsHTML(event.addedLabels, event.removedLabels))
            .appendParagraph(assigneesHTML(event.assignedPeople, event.unassignedPeople))
            .appendParagraph(reviewersHTML(event.addedReviewers, event.removedReviewers))
            .appendParagraph(event.rename?.let { renameHTML(it.first, it.second) }.orEmpty())

          Item(GiteeIcons.Timeline, GEPRTimelineItemComponentFactory.actionTitle(avatarIconsProvider, event.actor, "", event.createdAt),
               HtmlEditorPane(builder.toString()).apply {
                 border = JBUI.Borders.emptyLeft(28)
                 foreground = UIUtil.getContextHelpForeground()
               })
        }
        else -> throwUnknownType(event)
      }
    }

    private fun assigneesHTML(assigned: Collection<GEUser> = emptyList(), unassigned: Collection<GEUser> = emptyList()): String {
      val builder = StringBuilder()
      if (assigned.isNotEmpty()) {
        builder.append(
          assigned.joinToString(prefix = "${GiteeBundle.message("pull.request.timeline.assigned")} ") { "<b>${it.login}</b>" })
      }
      if (unassigned.isNotEmpty()) {
        if (builder.isNotEmpty()) builder.append(" ${GiteeBundle.message("pull.request.timeline.and")} ")
        builder.append(
          unassigned.joinToString(prefix = "${GiteeBundle.message("pull.request.timeline.unassigned")} ") { "<b>${it.login}</b>" })
      }
      return builder.toString()
    }

    private fun reviewersHTML(added: Collection<GEPullRequestRequestedReviewer?> = emptyList(),
                              removed: Collection<GEPullRequestRequestedReviewer?> = emptyList()): String {
      val builder = StringBuilder()
      if (added.isNotEmpty()) {
        builder.append(
          added.joinToString(prefix = "${GiteeBundle.message("pull.request.timeline.requested.review")} ") {
            "<b>${it?.shortName ?: GiteeBundle.message("user.someone")}</b>"
          })
      }
      if (removed.isNotEmpty()) {
        if (builder.isNotEmpty()) builder.append(" ${GiteeBundle.message("pull.request.timeline.and")} ")
        builder.append(removed.joinToString(
          prefix = "${GiteeBundle.message("pull.request.timeline.removed.review.request")} ") {
          "<b>${it?.shortName ?: GiteeBundle.message("user.someone")}</b>"
        })
      }
      return builder.toString()
    }

    private fun labelsHTML(added: Collection<GiteeIssueLabel> = emptyList(), removed: Collection<GiteeIssueLabel> = emptyList()): String {
      val builder = StringBuilder()
      if (added.isNotEmpty()) {
        if (added.size > 1) {
          builder.append(added.joinToString(prefix = "${GiteeBundle.message("pull.request.timeline.added.labels")} ") { labelHTML(it) })
        }
        else {
          builder.append(GiteeBundle.message("pull.request.timeline.added.label", labelHTML(added.first())))
        }
      }
      if (removed.isNotEmpty()) {
        if (builder.isNotEmpty()) builder.append(" ${GiteeBundle.message("pull.request.timeline.and")} ")
        if (removed.size > 1) {
          builder.append(
            removed.joinToString(prefix = "${GiteeBundle.message("pull.request.timeline.removed.labels")} ") { labelHTML(it) })
        }
        else {
          builder.append(GiteeBundle.message("pull.request.timeline.removed.label", labelHTML(removed.first())))
        }
      }
      return builder.toString()
    }

    private fun labelHTML(label: GiteeIssueLabel): String {
      val background = GEUIUtil.getLabelBackground(label)
      val foreground = GEUIUtil.getLabelForeground(background)
      //language=HTML
      return """<span style='color: #${ColorUtil.toHex(foreground)}; background: #${ColorUtil.toHex(background)}'>
                  &nbsp;${StringUtil.escapeXmlEntities(label.name)}&nbsp;</span>"""
    }

    private fun renameHTML(oldName: String, newName: String) = GiteeBundle.message("pull.request.timeline.renamed", oldName, newName)
  }

  private inner class StateEventComponentFactory : EventComponentFactory<GEPRTimelineEvent.State>() {
    override fun createComponent(event: GEPRTimelineEvent.State): Item {
      val icon = when (event.newState) {
        GEPullRequestState.CLOSED -> CollaborationToolsIcons.PullRequestClosed
        GEPullRequestState.MERGED -> GiteeIcons.PullRequestMerged
        GEPullRequestState.OPEN -> CollaborationToolsIcons.PullRequestOpen
      }

      val text = when (event.newState) {
        GEPullRequestState.CLOSED -> GiteeBundle.message("pull.request.timeline.closed")
        GEPullRequestState.MERGED -> {
          val mergeEvent = (if (event is GEPRTimelineMergedStateEvents) event.lastStateEvent else event) as GEPRMergedEvent
          if (mergeEvent.commit != null) {
            //language=HTML
            val commitText = """<a href='${mergeEvent.commit.url}'>${mergeEvent.commit.abbreviatedOid}</a>"""
            val ref = branchHTML(mergeEvent.mergeRefName)
            GiteeBundle.message("pull.request.timeline.merged.commit", commitText, ref)
          }
          else GiteeBundle.message("pull.request.timeline.merged")
        }
        GEPullRequestState.OPEN -> GiteeBundle.message("pull.request.timeline.reopened")
      }

      return eventItem(icon, event, text)
    }
  }

  private inner class BranchEventComponentFactory : EventComponentFactory<GEPRTimelineEvent.Branch>() {
    override fun createComponent(event: GEPRTimelineEvent.Branch): Item {
      return when (event) {
        is GEPRBaseRefChangedEvent ->
          eventItem(event, GiteeBundle.message("pull.request.timeline.changed.base.branch"))
        is GEPRBaseRefForcePushedEvent ->
          eventItem(event, GiteeBundle.message("pull.request.timeline.branch.force.pushed", branchHTML(event.ref) ?: "base"))

        is GEPRHeadRefForcePushedEvent ->
          eventItem(event, GiteeBundle.message("pull.request.timeline.branch.force.pushed", branchHTML(event.ref) ?: "head"))
        is GEPRHeadRefDeletedEvent ->
          eventItem(event, GiteeBundle.message("pull.request.timeline.branch.deleted", branchHTML(event.headRefName)))
        is GEPRHeadRefRestoredEvent ->
          eventItem(event, GiteeBundle.message("pull.request.timeline.branch.head.restored"))

        else -> throwUnknownType(event)
      }
    }

    private fun branchHTML(ref: GEGitRefName?) = ref?.name?.let { branchHTML(it) }
  }

  private inner class ComplexEventComponentFactory : EventComponentFactory<GEPRTimelineEvent.Complex>() {
    override fun createComponent(event: GEPRTimelineEvent.Complex): Item {
      return when (event) {
        is GEPRReviewDismissedEvent ->
          Item(GiteeIcons.Timeline,
               GEPRTimelineItemComponentFactory.actionTitle(avatarIconsProvider, event.actor,
                                                            GiteeBundle.message(
                                                              "pull.request.timeline.stale.review.dismissed",
                                                              event.reviewAuthor?.login
                                                              ?: GiteeBundle.message("pull.request.timeline.stale.review.author")),
                                                            event.createdAt),
               event.dismissalMessageHTML?.let {
                 HtmlEditorPane(it).apply {
                   border = JBUI.Borders.emptyLeft(28)
                 }
               })
        is GEPRReadyForReviewEvent ->
          Item(GiteeIcons.Review,
               GEPRTimelineItemComponentFactory.actionTitle(avatarIconsProvider, event.actor,
                                                            GiteeBundle.message("pull.request.timeline.marked.as.ready"),
                                                            event.createdAt))

        is GEPRConvertToDraftEvent ->
          Item(GiteeIcons.Review,
               GEPRTimelineItemComponentFactory.actionTitle(avatarIconsProvider, event.actor,
                                                            GiteeBundle.message("pull.request.timeline.marked.as.draft"),
                                                            event.createdAt))

        is GEPRCrossReferencedEvent -> {
          Item(GiteeIcons.Timeline,
               GEPRTimelineItemComponentFactory.actionTitle(avatarIconsProvider, event.actor,
                                                            GiteeBundle.message("pull.request.timeline.mentioned"),
                                                            event.createdAt),
               createComponent(event.source))
        }
        is GEPRConnectedEvent -> {
          Item(GiteeIcons.Timeline,
               GEPRTimelineItemComponentFactory.actionTitle(avatarIconsProvider, event.actor,
                                                            GiteeBundle.message("pull.request.timeline.connected"),
                                                            event.createdAt),
               createComponent(event.subject))
        }
        is GEPRDisconnectedEvent -> {
          Item(GiteeIcons.Timeline,
               GEPRTimelineItemComponentFactory.actionTitle(avatarIconsProvider, event.actor,
                                                            GiteeBundle.message("pull.request.timeline.disconnected"),
                                                            event.createdAt),
               createComponent(event.subject))
        }

        else -> throwUnknownType(event)
      }
    }
  }

  companion object {
    private fun branchHTML(name: String): String {
      val foreground = CurrentBranchComponent.TEXT_COLOR
      val background = CurrentBranchComponent.getBranchPresentationBackground(UIUtil.getListBackground())

      //language=HTML
      return """<span style='color: #${ColorUtil.toHex(foreground)}; background: #${ColorUtil.toHex(background)}'>
                  &nbsp;<icon-inline src='icons.CollaborationToolsIcons.Branch'/>$name&nbsp;</span>"""
    }

    private fun StringBuilder.appendParagraph(text: String): StringBuilder {
      if (text.isNotEmpty()) this.append("<p>").append(text).append("</p>")
      return this
    }

    private fun createComponent(reference: GEPRReferencedSubject): JComponent {
      val stateIcon = when (reference) {
        is GEPRReferencedSubject.Issue -> GEUIUtil.getIssueStateIcon(reference.state)
        is GEPRReferencedSubject.PullRequest -> GEUIUtil.getPullRequestStateIcon(reference.state, reference.isDraft)
      }
      val stateToolTip = when (reference) {
        is GEPRReferencedSubject.Issue -> GEUIUtil.getIssueStateText(reference.state)
        is GEPRReferencedSubject.PullRequest -> GEUIUtil.getPullRequestStateText(reference.state, reference.isDraft)
      }
      return NonOpaquePanel(HorizontalLayout(5)).apply {
        border = JBUI.Borders.emptyLeft(28)
        add(JLabel(stateIcon).apply {
          toolTipText = stateToolTip
        })
        //language=HTML
        add(HtmlEditorPane("""${reference.title}&nbsp<a href='${reference.url}'>#${reference.number}</a>"""))
      }
    }
  }
}
