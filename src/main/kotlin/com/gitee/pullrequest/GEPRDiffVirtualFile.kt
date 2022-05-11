// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest

import com.gitee.api.GERepositoryCoordinates
import com.gitee.i18n.GiteeBundle
import com.gitee.pullrequest.data.GEPRIdentifier
import com.intellij.diff.editor.DiffContentVirtualFile
import com.intellij.diff.editor.DiffVirtualFile.Companion.useDiffWindowDimensionKey
import com.intellij.ide.actions.SplitAction
import com.intellij.openapi.project.Project

@Suppress("EqualsOrHashCode")
internal class GEPRDiffVirtualFile(fileManagerId: String,
                                   project: Project,
                                   repository: GERepositoryCoordinates,
                                   pullRequest: GEPRIdentifier
)
  : GEPRVirtualFile(fileManagerId, project, repository, pullRequest), DiffContentVirtualFile {

  init {
    useDiffWindowDimensionKey()
    putUserData(SplitAction.FORBID_TAB_SPLIT, true)
    isWritable = false
  }

  override fun getName() = "#${pullRequest.number}.diff"
  override fun getPresentableName() = GiteeBundle.message("pull.request.diff.editor.title", pullRequest.number)

  override fun getPath(): String = (fileSystem as GEPRVirtualFileSystem).getPath(fileManagerId, project, repository, pullRequest, true)
  override fun getPresentablePath() = "${repository.toUrl()}/pulls/${pullRequest.number}.diff"

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is GEPRDiffVirtualFile) return false
    if (!super.equals(other)) return false
    return true
  }
}
