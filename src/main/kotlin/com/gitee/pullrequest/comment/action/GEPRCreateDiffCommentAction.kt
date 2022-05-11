// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.comment.action

import com.gitee.pullrequest.comment.GEPRDiffReviewSupport
import com.intellij.collaboration.ui.codereview.diff.CreateDiffCommentAction
import com.intellij.diff.tools.util.DiffDataKeys
import com.intellij.openapi.actionSystem.AnActionEvent

internal class GEPRCreateDiffCommentAction : CreateDiffCommentAction() {
  override fun isActive(e: AnActionEvent): Boolean {
    val request = e.getData(DiffDataKeys.DIFF_REQUEST) ?: return false

    return GEPRDiffReviewSupport.KEY.isIn(request)
  }
}