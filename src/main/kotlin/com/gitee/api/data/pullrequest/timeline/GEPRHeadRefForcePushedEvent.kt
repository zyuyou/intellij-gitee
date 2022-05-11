// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.pullrequest.timeline

import com.gitee.api.data.GEActor
import com.gitee.api.data.GECommitHash
import com.gitee.api.data.pullrequest.GEGitRefName
import java.util.*

class GEPRHeadRefForcePushedEvent(override val actor: GEActor?,
                                  override val createdAt: Date,
                                  val ref: GEGitRefName?,
                                  val beforeCommit: GECommitHash,
                                  val afterCommit: GECommitHash)
  : GEPRTimelineEvent.Branch
