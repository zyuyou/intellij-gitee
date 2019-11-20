// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.timeline

import com.gitee.api.data.GELabel
import com.gitee.api.data.GEUser
import com.gitee.api.data.pullrequest.GiteePullRequestReviewer
import com.gitee.api.data.pullrequest.timeline.*

class GiteePRTimelineMergedSimpleEvents : GiteePRTimelineMergedEvents<GiteePRTimelineEvent.Simple>(), GiteePRTimelineEvent.Simple {

  private val _addedLabels = mutableSetOf<GELabel>()
  val addedLabels: Set<GELabel> get() = _addedLabels
  private val _removedLabels = mutableSetOf<GELabel>()
  val removedLabels: Set<GELabel> get() = _removedLabels

  private val _assignedPeople = mutableSetOf<GEUser>()
  val assignedPeople: Set<GEUser> get() = _assignedPeople
  private val _unassignedPeople = mutableSetOf<GEUser>()
  val unassignedPeople: Set<GEUser> get() = _unassignedPeople

  private val _addedReviewers = mutableSetOf<GiteePullRequestReviewer>()
  val addedReviewers: Set<GiteePullRequestReviewer> get() = _addedReviewers
  private val _removedReviewers = mutableSetOf<GiteePullRequestReviewer>()
  val removedReviewers: Set<GiteePullRequestReviewer> get() = _removedReviewers

  private var _rename: Pair<String, String>? = null
  val rename: Pair<String, String>? get() = _rename?.let { if (it.first != it.second) it else null }

  override fun addNonMergedEvent(event: GiteePRTimelineEvent.Simple) {
    when (event) {
      is GiteePRLabeledEvent -> if (!_removedLabels.remove(event.label)) _addedLabels.add(event.label)
      is GiteePRUnlabeledEvent -> if (!_addedLabels.remove(event.label)) _removedLabels.add(event.label)

      is GiteePRAssignedEvent -> if (!_unassignedPeople.remove(event.user)) _assignedPeople.add(event.user)
      is GiteePRUnassignedEvent -> if (!_assignedPeople.remove(event.user)) _unassignedPeople.add(event.user)

      is GiteePRReviewRequestedEvent -> if (!_removedReviewers.remove(event.requestedReviewer)) _addedReviewers.add(event.requestedReviewer)
      is GiteePRReviewUnrequestedEvent -> if (!_addedReviewers.remove(event.requestedReviewer)) _removedReviewers.add(event.requestedReviewer)

      is GiteePRRenamedTitleEvent -> _rename = (_rename?.first ?: event.previousTitle) to event.currentTitle
    }
  }

  override fun hasAnyChanges(): Boolean =
    assignedPeople.isNotEmpty() || unassignedPeople.isNotEmpty() ||
    addedLabels.isNotEmpty() || removedLabels.isNotEmpty() ||
    addedReviewers.isNotEmpty() || removedReviewers.isNotEmpty() ||
    rename != null
}