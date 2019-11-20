// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.timeline

import com.gitee.api.data.pullrequest.GEPullRequestReview
import com.gitee.api.data.pullrequest.GEPullRequestReviewThread
import com.gitee.api.data.pullrequest.timeline.GiteePRTimelineEvent
import com.gitee.api.data.pullrequest.timeline.GiteePRTimelineItem
import com.intellij.util.text.DateFormatUtil
import javax.swing.AbstractListModel
import kotlin.math.max

class GiteePRTimelineMergingModel : AbstractListModel<GiteePRTimelineItem>(), GiteePRReviewsThreadsProvider {
  private val list = mutableListOf<GiteePRTimelineItem>()
  private var threadsByReview = mapOf<String, List<GEPullRequestReviewThread>>()
  private val threadsModelsByReview = mutableMapOf<String, GiteePRReviewThreadsModel>()

  override fun getElementAt(index: Int): GiteePRTimelineItem = list[index]

  override fun getSize(): Int = list.size

  override fun setReviewsThreads(threads: List<GEPullRequestReviewThread>) {
    threadsByReview = threads.groupBy { it.reviewId }
    for ((reviewId, model) in threadsModelsByReview) {
      model.update(threadsByReview[reviewId].orEmpty())
    }
  }

  override fun findReviewThreads(reviewId: String): GiteePRReviewThreadsModel? = threadsModelsByReview[reviewId]

  fun add(items: List<GiteePRTimelineItem>) {
    var lastListIdx = list.lastIndex
    var lastItem: GiteePRTimelineItem? = list.lastOrNull()
    if (lastItem != null) {
      list.removeAt(lastListIdx)
      fireIntervalRemoved(this, lastListIdx, lastListIdx)
      lastListIdx--
    }

    for (item in items) {
      val merged = mergeIfPossible(lastItem, item)
      if (merged != null) {
        lastItem = merged
      }
      else {
        if (lastItem != null && !isCollapsedMerge(lastItem)) list.add(lastItem)
        lastItem = item
      }

      if (item is GEPullRequestReview) {
        threadsModelsByReview[item.id] = GiteePRReviewThreadsModel(threadsByReview[item.id].orEmpty())
      }
    }
    if (lastItem != null && !isCollapsedMerge(lastItem)) list.add(lastItem)
    fireIntervalAdded(this, lastListIdx + 1, list.lastIndex)
  }

  fun removeAll() {
    val lastIdx = max(0, size - 1)
    list.clear()
    if (lastIdx > 0) fireIntervalRemoved(this, 0, lastIdx)

    threadsModelsByReview.clear()
  }

  companion object {
    private const val MERGE_THRESHOLD_MS = DateFormatUtil.MINUTE * 2

    private fun mergeIfPossible(existing: GiteePRTimelineItem?, new: GiteePRTimelineItem?): GiteePRTimelineEvent? {
      if (existing !is GiteePRTimelineEvent || new !is GiteePRTimelineEvent) return null

      if (existing.actor == new.actor && new.createdAt.time - existing.createdAt.time <= MERGE_THRESHOLD_MS) {
        if (existing is GiteePRTimelineEvent.Simple && new is GiteePRTimelineEvent.Simple) {
          if (existing is GiteePRTimelineMergedSimpleEvents) {
            existing.add(new)
            return existing
          }
          else {
            return GiteePRTimelineMergedSimpleEvents().apply {
              add(existing)
              add(new)
            }
          }
        }
        else if (existing is GiteePRTimelineEvent.State && new is GiteePRTimelineEvent.State) {
          if (existing is GiteePRTimelineMergedStateEvents) {
            existing.add(new)
            return existing
          }
          else {
            return GiteePRTimelineMergedStateEvents(existing).apply {
              add(new)
            }
          }
        }
      }
      return null
    }

    private fun isCollapsedMerge(event: GiteePRTimelineItem) = event is GiteePRTimelineMergedEvents<*> && !event.hasAnyChanges()
  }
}
