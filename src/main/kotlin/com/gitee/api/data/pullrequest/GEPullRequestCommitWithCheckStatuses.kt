// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.pullrequest

import com.gitee.api.data.GECommitWithCheckStatuses
import com.gitee.api.data.GENode

class GEPullRequestCommitWithCheckStatuses(id: String, val commit: GECommitWithCheckStatuses) : GENode(id)