// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.action

import com.gitee.api.data.pullrequest.GEPullRequestFileViewedState
import com.gitee.api.data.pullrequest.isViewed
import com.gitee.pullrequest.action.GEPRActionKeys.GIT_REPOSITORY
import com.gitee.pullrequest.action.GEPRActionKeys.PULL_REQUEST_DATA_PROVIDER
import com.gitee.pullrequest.action.GEPRActionKeys.PULL_REQUEST_FILES
import com.intellij.collaboration.messages.CollaborationToolsBundle.messagePointer
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.NlsActions.ActionText
import com.intellij.openapi.vcs.FilePath
import com.intellij.vcsUtil.VcsFileUtil.relativePath
import git4idea.repo.GitRepository
import java.util.function.Supplier

internal abstract class GEPRViewedStateAction(
  dynamicText: Supplier<@ActionText String>,
  private val isViewed: Boolean
) : DumbAwareAction(dynamicText) {

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false

    val repository = e.getData(GIT_REPOSITORY) ?: return
    val files = e.getData(PULL_REQUEST_FILES) ?: return
    val viewedStateProvider = e.getData(PULL_REQUEST_DATA_PROVIDER)?.viewedStateData ?: return
    val viewedState = viewedStateProvider.getViewedState()

    e.presentation.isEnabledAndVisible = files.any { viewedState.isViewed(repository, it) == !isViewed }
  }

  override fun actionPerformed(e: AnActionEvent) {
    val repository = e.getData(GIT_REPOSITORY)!!
    val files = e.getData(PULL_REQUEST_FILES)!!
    val viewedStateProvider = e.getData(PULL_REQUEST_DATA_PROVIDER)!!.viewedStateData
    val viewedState = viewedStateProvider.getViewedState()

    // todo seems we could make all mutations in single request
    for (file in files.filter { viewedState.isViewed(repository, it) == !isViewed }) {
      val repositoryRelativePath = relativePath(repository.root, file)

      viewedStateProvider.updateViewedState(repositoryRelativePath, isViewed)
    }
  }

  private fun Map<String, GEPullRequestFileViewedState>.isViewed(repository: GitRepository, file: FilePath): Boolean? {
    val repositoryRelativePath = relativePath(repository.root, file)

    return this[repositoryRelativePath]?.isViewed()
  }
}

internal class GEPRMarkFilesViewedAction :
  GEPRViewedStateAction(messagePointer("action.CodeReview.MarkChangesViewed.text"), true)

internal class GEPRMarkFilesNotViewedAction :
  GEPRViewedStateAction(messagePointer("action.CodeReview.MarkChangesNotViewed.text"), false)