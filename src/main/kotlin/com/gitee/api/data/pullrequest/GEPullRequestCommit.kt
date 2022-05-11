// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.pullrequest

import com.gitee.api.data.GECommit
import com.gitee.api.data.GENode
import com.gitee.api.data.pullrequest.timeline.GEPRTimelineItem

class GEPullRequestCommit(id: String,
                          val commit: GECommit,
                          val url: String)
  : GENode(id), GEPRTimelineItem