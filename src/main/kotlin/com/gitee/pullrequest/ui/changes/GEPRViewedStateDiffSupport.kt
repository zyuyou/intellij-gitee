// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.changes

import com.gitee.pullrequest.data.provider.GEPRViewedStateDataProvider
import com.intellij.openapi.util.Key
import com.intellij.openapi.vcs.FilePath
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.vcsUtil.VcsFileUtil.relativePath
import git4idea.repo.GitRepository

internal interface GEPRViewedStateDiffSupport {

  @RequiresEdt
  fun markViewed(file: FilePath)

  companion object {
    val KEY: Key<GEPRViewedStateDiffSupport> = Key.create("Gitee.PullRequest.Diff.ViewedState")
    val PULL_REQUEST_FILE: Key<FilePath> = Key.create("Gitee.PullRequest.Diff.File")
  }
}

internal class GEPRViewedStateDiffSupportImpl(
  private val repository: GitRepository,
  private val viewedStateData: GEPRViewedStateDataProvider
) : GEPRViewedStateDiffSupport {

  override fun markViewed(file: FilePath) {
    val repositoryRelativePath = relativePath(repository.root, file)

    viewedStateData.updateViewedState(repositoryRelativePath, true)
  }
}