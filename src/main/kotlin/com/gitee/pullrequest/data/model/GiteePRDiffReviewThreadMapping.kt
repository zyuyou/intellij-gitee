// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data.model

import com.gitee.api.data.pullrequest.GEPullRequestReviewThread
import com.intellij.diff.util.Side

class GiteePRDiffReviewThreadMapping(val side: Side, val fileLineIndex: Int, val thread: GEPullRequestReviewThread)