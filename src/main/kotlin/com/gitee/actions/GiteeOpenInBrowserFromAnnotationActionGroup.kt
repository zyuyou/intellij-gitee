/*
 *  Copyright 2016-2019 码云 - Gitee
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.gitee.actions

import com.gitee.util.GEProjectRepositoriesManager
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.components.service
import com.intellij.openapi.vcs.annotate.FileAnnotation
import com.intellij.openapi.vcs.annotate.UpToDateLineNumberListener
import com.intellij.vcsUtil.VcsUtil
import git4idea.GitUtil
import git4idea.annotate.GitFileAnnotation


/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/GithubOpenInBrowserFromAnnotationActionGroup.kt
 * @author JetBrains s.r.o.
 */
class GiteeOpenInBrowserFromAnnotationActionGroup(val annotation: FileAnnotation)
  : GiteeOpenInBrowserActionGroup(), UpToDateLineNumberListener {
  private var myLineNumber = -1

  override fun getData(dataContext: DataContext): List<Data>? {
    if (myLineNumber < 0) return null

    if (annotation !is GitFileAnnotation) return null
    val project = annotation.project
    val virtualFile = annotation.file

    val filePath = VcsUtil.getFilePath(virtualFile)
    val repository = GitUtil.getRepositoryManager(project).getRepositoryForFileQuick(filePath) ?: return null

    val accessibleRepositories = project.service<GEProjectRepositoriesManager>().findKnownRepositories(repository)
    if (accessibleRepositories.isEmpty()) return null

    val revisionHash = annotation.getLineRevisionNumber(myLineNumber)?.asString() ?: return null

    return accessibleRepositories.map { Data.Revision(project, it.geRepositoryCoordinates, revisionHash) }
  }

  override fun consume(integer: Int) {
    myLineNumber = integer
  }
}