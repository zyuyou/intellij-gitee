// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.timeline

import com.gitee.api.data.GEUser
import com.gitee.api.data.GiteeIssueLabel
import com.gitee.api.data.pullrequest.GEPullRequestRequestedReviewer
import com.gitee.api.data.pullrequest.timeline.*

class GEPRTimelineMergedSimpleEvents : GEPRTimelineMergedEvents<GEPRTimelineEvent.Simple>(), GEPRTimelineEvent.Simple {

  private val _addedLabels = mutableSetOf<GiteeIssueLabel>()
  val addedLabels: Set<GiteeIssueLabel> get() = _addedLabels
  private val _removedLabels = mutableSetOf<GiteeIssueLabel>()
  val removedLabels: Set<GiteeIssueLabel> get() = _removedLabels

  private val _assignedPeople = mutableSetOf<GEUser>()
  val assignedPeople: Set<GEUser> get() = _assignedPeople
  private val _unassignedPeople = mutableSetOf<GEUser>()
  val unassignedPeople: Set<GEUser> get() = _unassignedPeople

  private val _addedReviewers = mutableSetOf<GEPullRequestRequestedReviewer>()
  val addedReviewers: Set<GEPullRequestRequestedReviewer> get() = _addedReviewers
  private val _removedReviewers = mutableSetOf<GEPullRequestRequestedReviewer>()
  val removedReviewers: Set<GEPullRequestRequestedReviewer> get() = _removedReviewers

  private var _rename: Pair<String, String>? = null
  val rename: Pair<String, String>? get() = _rename?.let { if (it.first != it.second) it else null }

  override fun addNonMergedEvent(event: GEPRTimelineEvent.Simple) {
    when (event) {
      is GEPRLabeledEvent -> if (!_removedLabels.remove(event.label)) _addedLabels.add(event.label)
      is GEPRUnlabeledEvent -> if (!_addedLabels.remove(event.label)) _removedLabels.add(event.label)

      is GEPRAssignedEvent -> if (!_unassignedPeople.remove(event.user)) _assignedPeople.add(event.user)
      is GEPRUnassignedEvent -> if (!_assignedPeople.remove(event.user)) _unassignedPeople.add(event.user)

      is GEPRReviewRequestedEvent -> {
        val reviewer = event.requestedReviewer
        if (reviewer != null && !_removedReviewers.remove(reviewer)) _addedReviewers.add(reviewer)
      }
      is GEPRReviewUnrequestedEvent -> {
        val reviewer = event.requestedReviewer
        if (reviewer != null && !_addedReviewers.remove(reviewer)) _removedReviewers.add(reviewer)
      }

      is GEPRRenamedTitleEvent -> _rename = (_rename?.first ?: event.previousTitle) to event.currentTitle
    }
  }

  override fun hasAnyChanges(): Boolean =
    assignedPeople.isNotEmpty() || unassignedPeople.isNotEmpty() ||
    addedLabels.isNotEmpty() || removedLabels.isNotEmpty() ||
    addedReviewers.isNotEmpty() || removedReviewers.isNotEmpty() ||
    rename != null
}