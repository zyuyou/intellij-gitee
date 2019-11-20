// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.pullrequest.timeline

import com.gitee.api.data.GEActor
import com.gitee.api.data.GELabel
import java.util.*

class GiteePRUnlabeledEvent(override val actor: GEActor?,
                            override val createdAt: Date,
                            val label: GELabel)
  : GiteePRTimelineEvent.Simple