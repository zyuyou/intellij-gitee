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

package com.gitee.ui

import com.gitee.GiteeCreatePullRequestWorker.ForkInfo
import com.gitee.api.GiteeRepositoryPath
import com.gitee.util.GiteeNotifications
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.containers.Convertor
import javax.swing.JComponent

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/ui/GithubSelectForkDialog.java
 * @author JetBrains s.r.o.
 */
class GiteeSelectForkDialog(private val project: Project,
                            forks: List<GiteeRepositoryPath>?,
                            private val checkFork: Convertor<in String, out ForkInfo>) : DialogWrapper(project) {

  private val centerPanel: GiteeSelectForkPanel = GiteeSelectForkPanel()

  private var selectedFork: ForkInfo? = null

  init {
    if (forks != null) {
      centerPanel.setUsers(forks.map(GiteeRepositoryPath::owner))
    }

    title = "Select Base Fork Repository"

    init()
  }

  override fun createCenterPanel(): JComponent? {
    return centerPanel.panel
  }

  override fun doOKAction() {
    val fork = checkFork.convert(centerPanel.getUser())

    if (fork == null) {
      GiteeNotifications.showErrorDialog(project, "Can't Find Repository", "Can't find fork for selected user")
    } else {
      selectedFork = fork
      super.doOKAction()
    }
  }

  fun getPath(): ForkInfo {
    return selectedFork!!
  }

}