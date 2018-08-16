package com.gitee.ui

import com.gitee.api.GiteeFullPath
import com.gitee.util.GiteeCreatePullRequestWorker.ForkInfo
import com.gitee.util.GiteeNotifications
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.containers.Convertor
import javax.swing.JComponent

/**
 * Created by zyuyou on 2018/8/16.
 *
 */
class GiteeSelectForkDialog(private val project: Project,
                            forks: List<GiteeFullPath>?,
                            private val checkFork: Convertor<String, ForkInfo>) : DialogWrapper(project) {

  private val centerPanel: GiteeSelectForkPanel = GiteeSelectForkPanel()

  private var selectedFork: ForkInfo? = null

  init {
    if (forks != null) {
      centerPanel.setUsers(forks.map(GiteeFullPath::getUser))
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