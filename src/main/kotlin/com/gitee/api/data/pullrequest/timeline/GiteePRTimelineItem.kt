// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.pullrequest.timeline

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.gitee.api.data.GEIssueComment
import com.gitee.api.data.pullrequest.GEPullRequestCommit
import com.gitee.api.data.pullrequest.GEPullRequestReview
import com.gitee.api.data.pullrequest.timeline.GiteePRTimelineItem.Unknown

/*REQUIRED
IssueComment
PullRequestCommit (Commit in GHE)
PullRequestReview

RenamedTitleEvent
ClosedEvent | ReopenedEvent | MergedEvent
AssignedEvent | UnassignedEvent
LabeledEvent | UnlabeledEvent
ReviewRequestedEvent | ReviewRequestRemovedEvent
ReviewDismissedEvent

BaseRefChangedEvent | BaseRefForcePushedEvent
HeadRefDeletedEvent | HeadRefForcePushedEvent | HeadRefRestoredEvent
*/
/*MAYBE
LockedEvent | UnlockedEvent

CommentDeletedEvent
???PullRequestCommitCommentThread
???PullRequestReviewThread
AddedToProjectEvent
ConvertedNoteToIssueEvent
RemovedFromProjectEvent
MovedColumnsInProjectEvent

TransferredEvent
UserBlockedEvent

PullRequestRevisionMarker

DeployedEvent
DeploymentEnvironmentChangedEvent
PullRequestReviewThread
PinnedEvent | UnpinnedEvent
SubscribedEvent | UnsubscribedEvent
MilestonedEvent | DemilestonedEvent
MentionedEvent | ReferencedEvent | CrossReferencedEvent
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "__typename", visible = false,
              defaultImpl = Unknown::class)
@JsonSubTypes(
  JsonSubTypes.Type(name = "IssueComment", value = GEIssueComment::class),
  JsonSubTypes.Type(name = "PullRequestCommit", value = GEPullRequestCommit::class),
  JsonSubTypes.Type(name = "PullRequestReview", value = GEPullRequestReview::class),

  JsonSubTypes.Type(name = "ReviewDismissedEvent", value = GiteePRReviewDismissedEvent::class),

  JsonSubTypes.Type(name = "RenamedTitleEvent", value = GiteePRRenamedTitleEvent::class),

  JsonSubTypes.Type(name = "ClosedEvent", value = GiteePRClosedEvent::class),
  JsonSubTypes.Type(name = "ReopenedEvent", value = GiteePRReopenedEvent::class),
  JsonSubTypes.Type(name = "MergedEvent", value = GiteePRMergedEvent::class),

  JsonSubTypes.Type(name = "AssignedEvent", value = GiteePRAssignedEvent::class),
  JsonSubTypes.Type(name = "UnassignedEvent", value = GiteePRUnassignedEvent::class),

  JsonSubTypes.Type(name = "LabeledEvent", value = GiteePRLabeledEvent::class),
  JsonSubTypes.Type(name = "UnlabeledEvent", value = GiteePRUnlabeledEvent::class),

  JsonSubTypes.Type(name = "ReviewRequestedEvent", value = GiteePRReviewRequestedEvent::class),
  JsonSubTypes.Type(name = "ReviewRequestRemovedEvent", value = GiteePRReviewUnrequestedEvent::class),

  JsonSubTypes.Type(name = "BaseRefChangedEvent", value = GiteePRBaseRefChangedEvent::class),
  JsonSubTypes.Type(name = "BaseRefForcePushedEvent", value = GiteePRBaseRefForcePushedEvent::class),

  JsonSubTypes.Type(name = "HeadRefDeletedEvent", value = GiteePRHeadRefDeletedEvent::class),
  JsonSubTypes.Type(name = "HeadRefForcePushedEvent", value = GiteePRHeadRefForcePushedEvent::class),
  JsonSubTypes.Type(name = "HeadRefRestoredEvent", value = GiteePRHeadRefRestoredEvent::class)
)
interface GiteePRTimelineItem {
  class Unknown : GiteePRTimelineItem
}