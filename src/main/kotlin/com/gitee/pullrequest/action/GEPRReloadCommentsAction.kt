// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.gitee.pullrequest.action

import com.gitee.i18n.GiteeBundle
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.RefreshAction
import com.intellij.openapi.actionSystem.AnActionEvent
import java.util.function.Supplier

class GEPRReloadCommentsAction
  : RefreshAction(
  GiteeBundle.messagePointer("pull.request.refresh.comments.action"),
                  Supplier<String?> { null },
                  AllIcons.Actions.Refresh) {

  override fun update(e: AnActionEvent) {
    val selection = e.getData(GEPRActionKeys.PULL_REQUEST_DATA_PROVIDER)
    e.presentation.isEnabled = selection != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    e.getRequiredData(GEPRActionKeys.PULL_REQUEST_DATA_PROVIDER).reviewData.resetReviewThreads()
  }
}