// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.actions

import com.gitee.util.GEHostedRepositoriesManager
import com.intellij.ide.actions.DumbAwareCopyPathProvider
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vfs.VirtualFile
import git4idea.GitUtil
import git4idea.remote.hosting.findKnownRepositories

class GiteeCopyPathProvider: DumbAwareCopyPathProvider() {
  override fun getPathToElement(project: Project, virtualFile: VirtualFile?, editor: Editor?): String? {
    if (virtualFile == null) return null

    val fileStatus = ChangeListManager.getInstance(project).getStatus(virtualFile)
    if (fileStatus == FileStatus.UNKNOWN || fileStatus == FileStatus.ADDED || fileStatus == FileStatus.IGNORED) return null

    val repository = GitUtil.getRepositoryManager(project).getRepositoryForFileQuick(virtualFile) ?: return null

    val accessibleRepositories = project.service<GEHostedRepositoriesManager>().findKnownRepositories(repository)
    if (accessibleRepositories.isEmpty()) return null

    val refs = accessibleRepositories
      .mapNotNull { GEPathUtil.getFileURL(repository, it.repository, virtualFile, editor) }
      .distinct()

    return if (refs.isNotEmpty()) refs.joinToString("\n") else null
  }
}