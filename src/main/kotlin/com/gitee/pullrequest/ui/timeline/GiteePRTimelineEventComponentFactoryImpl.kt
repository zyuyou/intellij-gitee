// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.timeline

import com.gitee.api.data.GELabel
import com.gitee.api.data.GEUser
import com.gitee.api.data.pullrequest.GiteeGitRefName
import com.gitee.api.data.pullrequest.GiteePullRequestReviewer
import com.gitee.api.data.pullrequest.GiteePullRequestState
import com.gitee.api.data.pullrequest.timeline.*
import com.gitee.icons.GiteeIcons
import com.gitee.pullrequest.avatars.GiteeAvatarIconsProvider
import com.gitee.pullrequest.ui.timeline.GiteePRTimelineItemComponentFactory.Item
import com.gitee.ui.util.HtmlEditorPane
import com.gitee.util.GiteeUIUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vcs.changes.ui.CurrentBranchComponent
import com.intellij.ui.ColorUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.intellij.lang.annotations.Language
import javax.swing.Icon

class GiteePRTimelineEventComponentFactoryImpl(private val avatarIconsProvider: GiteeAvatarIconsProvider)
  : GiteePRTimelineEventComponentFactory<GiteePRTimelineEvent> {

  private val simpleEventDelegate = SimpleEventComponentFactory()
  private val stateEventDelegate = StateEventComponentFactory()
  private val branchEventDelegate = BranchEventComponentFactory()
  private val complexEventDelegate = ComplexEventComponentFactory()

  override fun createComponent(event: GiteePRTimelineEvent): Item {
    return when (event) {
      is GiteePRTimelineEvent.Simple -> simpleEventDelegate.createComponent(event)
      is GiteePRTimelineEvent.State -> stateEventDelegate.createComponent(event)
      is GiteePRTimelineEvent.Branch -> branchEventDelegate.createComponent(event)
      is GiteePRTimelineEvent.Complex -> complexEventDelegate.createComponent(event)
      else -> throwUnknownType(event)
    }
  }

  private fun throwUnknownType(item: GiteePRTimelineEvent): Nothing {
    throw IllegalStateException("""Unknown event type "${item.javaClass.canonicalName}"""")
  }

  private abstract inner class EventComponentFactory<T : GiteePRTimelineEvent> : GiteePRTimelineEventComponentFactory<T> {

    protected fun eventItem(item: GiteePRTimelineEvent, @Language("HTML") titleHTML: String): Item {
      return eventItem(GiteeIcons.Timeline, item, titleHTML)
    }

    protected fun eventItem(markerIcon: Icon,
                            item: GiteePRTimelineEvent,
                            @Language("HTML") titleHTML: String): Item {
      return Item(markerIcon, GiteePRTimelineItemComponentFactory.actionTitle(avatarIconsProvider, item.actor, titleHTML, item.createdAt))
    }
  }

  private inner class SimpleEventComponentFactory : EventComponentFactory<GiteePRTimelineEvent.Simple>() {
    override fun createComponent(event: GiteePRTimelineEvent.Simple): Item {
      return when (event) {
        is GiteePRAssignedEvent ->
          eventItem(event, assigneesHTML(assigned = listOf(event.user)))
        is GiteePRUnassignedEvent ->
          eventItem(event, assigneesHTML(unassigned = listOf(event.user)))

        is GiteePRReviewRequestedEvent ->
          eventItem(event, reviewersHTML(added = listOf(event.requestedReviewer)))
        is GiteePRReviewUnrequestedEvent ->
          eventItem(event, reviewersHTML(removed = listOf(event.requestedReviewer)))

        is GiteePRLabeledEvent ->
          eventItem(event, labelsHTML(added = listOf(event.label)))
        is GiteePRUnlabeledEvent ->
          eventItem(event, labelsHTML(removed = listOf(event.label)))

        is GiteePRRenamedTitleEvent ->
          eventItem(event, renameHTML(event.previousTitle, event.currentTitle))

        is GiteePRTimelineMergedSimpleEvents -> {
          val builder = StringBuilder()
            .appendParagraph(labelsHTML(event.addedLabels, event.removedLabels))
            .appendParagraph(assigneesHTML(event.assignedPeople, event.unassignedPeople))
            .appendParagraph(reviewersHTML(event.addedReviewers, event.removedReviewers))
            .appendParagraph(event.rename?.let { renameHTML(it.first, it.second) }.orEmpty())

          Item(GiteeIcons.Timeline, GiteePRTimelineItemComponentFactory.actionTitle(avatarIconsProvider, event.actor, "", event.createdAt),
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
        builder.append(assigned.joinToString(prefix = "assigned ") { "<b>${it.login}</b>" })
      }
      if (unassigned.isNotEmpty()) {
        if (builder.isNotEmpty()) builder.append(" and ")
        builder.append(unassigned.joinToString(prefix = "unassigned ") { "<b>${it.login}</b>" })
      }
      return builder.toString()
    }

    private fun reviewersHTML(added: Collection<GiteePullRequestReviewer> = emptyList(),
                              removed: Collection<GiteePullRequestReviewer> = emptyList()): String {
      val builder = StringBuilder()
      if (added.isNotEmpty()) {
        builder.append(added.joinToString(prefix = "requested a review from ") { "<b>${extractReviewerName(it)}</b>" })
      }
      if (removed.isNotEmpty()) {
        if (builder.isNotEmpty()) builder.append(" and ")
        builder.append(removed.joinToString(prefix = "removed review request from ") { "<b>${extractReviewerName(it)}</b>" })
      }
      return builder.toString()
    }

    private fun extractReviewerName(reviewer: GiteePullRequestReviewer): String {
      return when (reviewer) {
        is GEUser -> reviewer.login
        is GiteePullRequestReviewer.Team -> "team"
        else -> throw IllegalArgumentException()
      }
    }

    private fun labelsHTML(added: Collection<GELabel> = emptyList(), removed: Collection<GELabel> = emptyList()): String {
      val builder = StringBuilder()
      if (added.isNotEmpty()) {
        if (added.size > 1) {
          builder.append(added.joinToString(prefix = "added labels ") { labelHTML(it) })
        }
        else {
          builder.append("added the ").append(labelHTML(added.first())).append(" label")
        }
      }
      if (removed.isNotEmpty()) {
        if (builder.isNotEmpty()) builder.append(" and ")
        if (removed.size > 1) {
          builder.append(removed.joinToString(prefix = "removed labels ") { labelHTML(it) })
        }
        else {
          builder.append("removed the ").append(labelHTML(removed.first())).append(" label")
        }
      }
      return builder.toString()
    }

    private fun labelHTML(label: GELabel): String {
      val background = GiteeUIUtil.getLabelBackground(label)
      val foreground = GiteeUIUtil.getLabelForeground(background)
      //language=HTML
      return """<span style='color: #${ColorUtil.toHex(foreground)}; background: #${ColorUtil.toHex(background)}'>
                  &nbsp;${StringUtil.escapeXmlEntities(label.name)}&nbsp;</span>"""
    }

    private fun renameHTML(oldName: String, newName: String) = "renamed this from <b>$oldName</b> to <b>$newName</b>"
  }

  private inner class StateEventComponentFactory : EventComponentFactory<GiteePRTimelineEvent.State>() {
    override fun createComponent(event: GiteePRTimelineEvent.State): Item {
      val icon = when (event.newState) {
        GiteePullRequestState.CLOSED -> GiteeIcons.PullRequestClosed
        GiteePullRequestState.MERGED -> GiteeIcons.PullRequestMerged
        GiteePullRequestState.OPEN -> GiteeIcons.PullRequestOpen
      }

      val text = when (event.newState) {
        GiteePullRequestState.CLOSED -> "closed this"
        GiteePullRequestState.MERGED -> "merged this"
        GiteePullRequestState.OPEN -> "reopened this"
      }

      return eventItem(icon, event, text)
    }
  }

  private inner class BranchEventComponentFactory : EventComponentFactory<GiteePRTimelineEvent.Branch>() {
    override fun createComponent(event: GiteePRTimelineEvent.Branch): Item {
      return when (event) {
        is GiteePRBaseRefChangedEvent ->
          eventItem(event, "changed the base branch")
        is GiteePRBaseRefForcePushedEvent ->
          eventItem(event, "force-pushed the ${branchHTML(event.ref) ?: "base"} branch")

        is GiteePRHeadRefForcePushedEvent ->
          eventItem(event, "force-pushed the ${branchHTML(event.ref) ?: "head"} branch")
        is GiteePRHeadRefDeletedEvent ->
          eventItem(event, "deleted the ${branchHTML(event.headRefName)} branch")
        is GiteePRHeadRefRestoredEvent ->
          eventItem(event, "restored head branch")

        else -> throwUnknownType(event)
      }
    }

    private fun branchHTML(ref: GiteeGitRefName?) = ref?.name?.let { branchHTML(it) }

    //language=HTML
    private fun branchHTML(name: String): String {
      val foreground = CurrentBranchComponent.TEXT_COLOR
      val background = CurrentBranchComponent.getBranchPresentationBackground(UIUtil.getListBackground())

      return """<span style='color: #${ColorUtil.toHex(foreground)}; background: #${ColorUtil.toHex(background)}'>
                  &nbsp;<icon-inline src='GiteeIcons.Branch'/>$name&nbsp;</span>"""
    }
  }

  private inner class ComplexEventComponentFactory : EventComponentFactory<GiteePRTimelineEvent.Complex>() {
    override fun createComponent(event: GiteePRTimelineEvent.Complex): Item {
      return when (event) {
        is GiteePRReviewDismissedEvent ->
          Item(GiteeIcons.Timeline, GiteePRTimelineItemComponentFactory.actionTitle(avatarIconsProvider, event.actor,
                                                                                  "dismissed <b>${event.reviewAuthor?.login}</b>`s stale review",
                                                                                  event.createdAt),
               event.dismissalMessageHTML?.let {
                 HtmlEditorPane(it).apply {
                   border = JBUI.Borders.emptyLeft(28)
                 }
               })

        else -> throwUnknownType(event)
      }
    }
  }

  companion object {
    private fun StringBuilder.appendParagraph(text: String): StringBuilder {
      if (text.isNotEmpty()) this.append("<p>").append(text).append("</p>")
      return this
    }
  }
}
