// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.request

import com.intellij.diff.util.Side

data class GEPullRequestDraftReviewThread(val body: String, val line: Int, val path: String, val side: Side, val startLine: Int?, val startSide: Side?)