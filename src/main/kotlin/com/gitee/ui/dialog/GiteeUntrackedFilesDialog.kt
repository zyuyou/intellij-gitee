package com.gitee.ui.dialog

import com.gitee.i18n.GiteeBundle
import com.intellij.CommonBundle
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Splitter
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.ui.SelectFilesDialog
import com.intellij.openapi.vcs.ui.CommitMessage
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.annotations.NonNls
import javax.swing.JComponent

class GiteeUntrackedFilesDialog(private val myProject: Project, untrackedFiles: List<VirtualFile>) :
  SelectFilesDialog(myProject, untrackedFiles, null, null, true, false),
  DataProvider {
  private var myCommitMessagePanel: CommitMessage? = null

  val commitMessage: String
    get() = myCommitMessagePanel!!.comment

  init {
    title = GiteeBundle.message("untracked.files.dialog.title")
    setOKButtonText(CommonBundle.getAddButtonText())
    setCancelButtonText(CommonBundle.getCancelButtonText())
    init()
  }

  override fun createNorthPanel(): JComponent? {
    return null
  }

  override fun createCenterPanel(): JComponent? {
    val tree = super.createCenterPanel()

    val commitMessage = CommitMessage(myProject)
    Disposer.register(disposable, commitMessage)
    commitMessage.setCommitMessage("Initial commit")
    myCommitMessagePanel = commitMessage

    val splitter = Splitter(true)
    splitter.setHonorComponentsMinimumSize(true)
    splitter.firstComponent = tree
    splitter.secondComponent = myCommitMessagePanel
    splitter.proportion = 0.7f

    return splitter
  }

  override fun getData(@NonNls dataId: String): Any? {
    return if (VcsDataKeys.COMMIT_MESSAGE_CONTROL.`is`(dataId)) {
      myCommitMessagePanel
    } else null
  }

  override fun getDimensionServiceKey(): String? {
    return "Gitee.UntrackedFilesDialog"
  }
}