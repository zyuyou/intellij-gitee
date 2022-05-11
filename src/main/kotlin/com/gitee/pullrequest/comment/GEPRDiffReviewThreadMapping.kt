// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.comment

import com.gitee.api.data.pullrequest.GEPullRequestReviewThread
import com.intellij.diff.util.Side

class GEPRDiffReviewThreadMapping(val diffSide: Side, val fileLineIndex: Int,
                                  val thread: GEPullRequestReviewThread
)