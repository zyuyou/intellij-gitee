// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.timeline

import com.gitee.api.data.GEActor
import com.gitee.api.data.pullrequest.timeline.GEPRTimelineEvent
import java.util.*

abstract class GEPRTimelineMergedEvents<T : GEPRTimelineEvent>
  : GEPRTimelineEvent {

  private val events = mutableListOf<T>()

  override val actor: GEActor?
    get() = events.last().actor
  override val createdAt: Date
    get() = events.last().createdAt

  fun add(event: T) {
    events.add(event)
    if (event is GEPRTimelineMergedEvents<*>) {
      for (evt in event.events) {
        @Suppress("UNCHECKED_CAST")
        add(evt as T)
      }
    }
    else {
      addNonMergedEvent(event)
    }
  }

  protected abstract fun addNonMergedEvent(event: T)

  abstract fun hasAnyChanges(): Boolean
}