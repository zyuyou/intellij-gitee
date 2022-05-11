// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.changes

import com.gitee.pullrequest.data.GEPRChangedFileLinesMapper
import com.intellij.diff.util.Side

class GEPRCreateDiffCommentParametersHelper(val commitSha: String, val filePath: String,
                                            private val linesMapper: GEPRChangedFileLinesMapper
) {

  fun findPosition(diffSide: Side, sideFileLine: Int): Int? = linesMapper.findDiffLine(diffSide, sideFileLine)
}
