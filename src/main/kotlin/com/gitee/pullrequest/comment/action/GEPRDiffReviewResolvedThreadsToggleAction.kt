// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.comment.action

import com.gitee.i18n.GiteeBundle
import com.gitee.pullrequest.comment.GEPRDiffReviewSupport
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction

class GEPRDiffReviewResolvedThreadsToggleAction
  : ToggleAction({ GiteeBundle.message("pull.request.review.show.resolved.threads") },
                 { GiteeBundle.message("pull.request.review.show.resolved.threads.description") },
                 null) {

  override fun update(e: AnActionEvent) {
    super.update(e)
    val reviewSupport = e.getData(GEPRDiffReviewSupport.DATA_KEY)
    e.presentation.isVisible = reviewSupport != null
    e.presentation.isEnabled = reviewSupport != null && reviewSupport.showReviewThreads
  }

  override fun isSelected(e: AnActionEvent): Boolean =
    e.getData(GEPRDiffReviewSupport.DATA_KEY)?.showResolvedReviewThreads ?: false

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    e.getData(GEPRDiffReviewSupport.DATA_KEY)?.showResolvedReviewThreads = state
  }
}