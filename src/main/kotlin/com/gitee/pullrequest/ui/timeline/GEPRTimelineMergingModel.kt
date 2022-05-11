// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.timeline

import com.gitee.api.data.pullrequest.timeline.GEPRTimelineEvent
import com.gitee.api.data.pullrequest.timeline.GEPRTimelineItem
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.text.DateFormatUtil
import javax.swing.AbstractListModel
import kotlin.math.max

class GEPRTimelineMergingModel : AbstractListModel<GEPRTimelineItem>() {
  private val list = mutableListOf<GEPRTimelineItem>()

  override fun getElementAt(index: Int): GEPRTimelineItem = list[index]

  override fun getSize(): Int = list.size

  fun add(items: List<GEPRTimelineItem>) {
    var lastListIdx = list.lastIndex
    var lastItem: GEPRTimelineItem? = list.lastOrNull()
    if (lastItem != null) {
      list.removeAt(lastListIdx)
      fireIntervalRemoved(this, lastListIdx, lastListIdx)
      lastListIdx--
    }

    var added = false
    val hideUnknown = ApplicationManager.getApplication().let { !it.isInternal && !it.isEAP }
    for (item in items) {
      if (item is GEPRTimelineItem.Unknown && (hideUnknown || item.__typename in GEPRTimelineItem.IGNORED_TYPES)) continue
      val merged = mergeIfPossible(lastItem, item)
      if (merged != null) {
        lastItem = merged
      }
      else {
        if (lastItem != null && !isCollapsedMerge(lastItem)) {
          list.add(lastItem)
          added = true
        }
        lastItem = item
      }
    }
    if (lastItem != null && !isCollapsedMerge(lastItem)) {
      list.add(lastItem)
      added = true
    }
    if (added) fireIntervalAdded(this, lastListIdx + 1, list.lastIndex)
  }

  fun update(item: GEPRTimelineItem) {
    val idx = list.indexOf(item)
    if (idx >= 0) list[idx] = item
    fireContentsChanged(this, idx, idx)
  }

  fun remove(item: GEPRTimelineItem) {
    val idx = list.indexOf(item)
    if (idx >= 0) list.removeAt(idx)
    fireIntervalRemoved(this, idx, idx)
  }

  fun removeAll() {
    val lastIdx = max(0, size - 1)
    list.clear()
    if (lastIdx > 0) fireIntervalRemoved(this, 0, lastIdx)
  }

  companion object {
    private const val MERGE_THRESHOLD_MS = DateFormatUtil.MINUTE * 2

    private fun mergeIfPossible(existing: GEPRTimelineItem?, new: GEPRTimelineItem?): GEPRTimelineEvent? {
      if (existing !is GEPRTimelineEvent || new !is GEPRTimelineEvent) return null

      if (existing.actor == new.actor && new.createdAt.time - existing.createdAt.time <= MERGE_THRESHOLD_MS) {
        if (existing is GEPRTimelineEvent.Simple && new is GEPRTimelineEvent.Simple) {
          if (existing is GEPRTimelineMergedSimpleEvents) {
            existing.add(new)
            return existing
          }
          else {
            return GEPRTimelineMergedSimpleEvents().apply {
              add(existing)
              add(new)
            }
          }
        }
        else if (existing is GEPRTimelineEvent.State && new is GEPRTimelineEvent.State) {
          if (existing is GEPRTimelineMergedStateEvents) {
            existing.add(new)
            return existing
          }
          else {
            return GEPRTimelineMergedStateEvents(existing).apply {
              add(new)
            }
          }
        }
      }
      return null
    }

    private fun isCollapsedMerge(event: GEPRTimelineItem) = event is GEPRTimelineMergedEvents<*> && !event.hasAnyChanges()
  }
}
