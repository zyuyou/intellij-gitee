// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data

import com.gitee.api.data.pullrequest.timeline.GEPRTimelineItem
import java.util.*

open class GEIssueComment(id: String,
                          author: GEActor?,
                          body: String,
                          createdAt: Date,
                          val viewerCanDelete: Boolean,
                          val viewerCanUpdate: Boolean)
  : GEComment(id, author, body, createdAt), GEPRTimelineItem
