// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data

import com.gitee.api.data.pullrequest.timeline.GiteePRTimelineItem
import java.util.*

open class GEIssueComment(id: String,
                          val url: String,
                          author: GEActor?,
                          bodyHTML: String,
                          createdAt: Date)
  : GEComment(id, author, bodyHTML, createdAt), GiteePRTimelineItem
