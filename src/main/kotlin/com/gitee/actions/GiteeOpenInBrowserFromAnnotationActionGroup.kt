// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.actions

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vcs.annotate.FileAnnotation
import com.intellij.openapi.vcs.annotate.UpToDateLineNumberListener
import git4idea.GitUtil
import com.gitee.api.GiteeRepositoryPath
import com.gitee.util.GiteeGitHelper


class GiteeOpenInBrowserFromAnnotationActionGroup(val annotation: FileAnnotation) : com.gitee.actions.GiteeOpenInBrowserActionGroup(), UpToDateLineNumberListener {
  private var myLineNumber = -1

  override fun getData(dataContext: DataContext): Pair<Set<GiteeRepositoryPath>, Data>? {
    if (myLineNumber < 0) return null

    val project = dataContext.getData(CommonDataKeys.PROJECT)
    val virtualFile = dataContext.getData(CommonDataKeys.VIRTUAL_FILE)
    if (project == null || virtualFile == null) return null

    FileDocumentManager.getInstance().getDocument(virtualFile) ?: return null

    val repository = GitUtil.getRepositoryManager(project).getRepositoryForFile(virtualFile) ?: return null

    val accessibleRepositories = service<GiteeGitHelper>().getPossibleRepositories(repository)
    if (accessibleRepositories.isEmpty()) return null

    val revisionHash = annotation.getLineRevisionNumber(myLineNumber)?.asString() ?: return null

    return accessibleRepositories to Data.Revision(project, revisionHash)
  }

  override fun consume(integer: Int) {
    myLineNumber = integer
  }
}
