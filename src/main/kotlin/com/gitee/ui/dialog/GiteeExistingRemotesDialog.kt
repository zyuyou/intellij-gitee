package com.gitee.ui.dialog

import com.gitee.i18n.GiteeBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.BrowserLink
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Container
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

class GiteeExistingRemotesDialog(project: Project, private val remotes: List<String>) : DialogWrapper(project) {
  init {
    title = GiteeBundle.message("share.error.project.is.on.gitee")
    setOKButtonText(GiteeBundle.message("share.anyway.button"))
    init()
  }

  override fun createCenterPanel(): JComponent? {
    val mainText = JBLabel(
      if (remotes.size == 1)
        GiteeBundle.message("share.action.remote.is.on.gitee")
      else
        GiteeBundle.message("share.action.remotes.are.on.gitee")
    )

    val remotesPanel = JPanel().apply {
      layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }
    for (remote in remotes) {
      remotesPanel.add(BrowserLink(remote, remote))
    }

    val messagesPanel = JBUI.Panels.simplePanel(UIUtil.DEFAULT_HGAP, UIUtil.DEFAULT_VGAP)
      .addToTop(mainText)
      .addToCenter(remotesPanel)

    val iconContainer = Container().apply {
      layout = BorderLayout()
      add(JLabel(Messages.getQuestionIcon()), BorderLayout.NORTH)
    }

    return JBUI.Panels.simplePanel(UIUtil.DEFAULT_HGAP, UIUtil.DEFAULT_VGAP)
      .addToCenter(messagesPanel)
      .addToLeft(iconContainer)
      .apply { border = JBUI.Borders.emptyBottom(UIUtil.LARGE_VGAP) }
  }
}