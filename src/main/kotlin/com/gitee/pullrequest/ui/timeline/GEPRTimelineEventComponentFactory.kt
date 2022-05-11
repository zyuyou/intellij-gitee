// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.timeline

import com.gitee.api.data.pullrequest.timeline.GEPRTimelineEvent

interface GEPRTimelineEventComponentFactory<T : GEPRTimelineEvent> {
  fun createComponent(event: T): GEPRTimelineItemComponentFactory.Item
}
