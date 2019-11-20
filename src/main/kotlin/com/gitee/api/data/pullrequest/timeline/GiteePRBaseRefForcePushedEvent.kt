// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.pullrequest.timeline

import com.gitee.api.data.GEActor
import com.gitee.api.data.GECommitHash
import com.gitee.api.data.pullrequest.GiteeGitRefName
import java.util.*

class GiteePRBaseRefForcePushedEvent(override val actor: GEActor?,
                                     override val createdAt: Date,
                                     val ref: GiteeGitRefName?,
                                     val beforeCommit: GECommitHash,
                                     val afterCommit: GECommitHash)
  : GiteePRTimelineEvent.Branch
