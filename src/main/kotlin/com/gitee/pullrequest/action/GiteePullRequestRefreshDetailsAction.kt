// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.gitee.pullrequest.action

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction

class GiteePullRequestRefreshDetailsAction : DumbAwareAction("Refresh Pull Request Details", null, AllIcons.Actions.Refresh) {
  override fun update(e: AnActionEvent) {
    val selection = e.getData(GiteePullRequestKeys.ACTION_DATA_CONTEXT)?.selectedPullRequestDataProvider
    e.presentation.isEnabled = selection != null
  }

  override fun actionPerformed(e: AnActionEvent) {
    e.getRequiredData(GiteePullRequestKeys.ACTION_DATA_CONTEXT).selectedPullRequestDataProvider?.reloadDetails()
  }
}