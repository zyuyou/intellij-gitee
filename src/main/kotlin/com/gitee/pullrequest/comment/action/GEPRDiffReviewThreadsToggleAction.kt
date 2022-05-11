// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.comment.action

import com.gitee.i18n.GiteeBundle
import com.gitee.pullrequest.comment.GEPRDiffReviewSupport
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction

class GEPRDiffReviewThreadsToggleAction
  : ToggleAction({ GiteeBundle.message("pull.request.review.show.comments.action") },
                 { GiteeBundle.message("pull.request.review.show.comments.action.description") },
                 null) {

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isEnabledAndVisible = e.getData(GEPRDiffReviewSupport.DATA_KEY) != null
  }

  override fun isSelected(e: AnActionEvent): Boolean =
    e.getData(GEPRDiffReviewSupport.DATA_KEY)?.showReviewThreads ?: false

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    e.getData(GEPRDiffReviewSupport.DATA_KEY)?.showReviewThreads = state
  }
}