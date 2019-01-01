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
package com.gitee.pullrequest.ui

import com.gitee.api.data.GiteeIssueState
import com.gitee.api.data.GiteePullRequest
import com.gitee.icons.GiteeIcons
import com.gitee.pullrequest.avatars.CachingGiteeAvatarIconsProvider
import com.gitee.util.GiteeUIUtil
import com.intellij.ide.CopyProvider
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.util.Disposer
import com.intellij.ui.ListUtil
import com.intellij.ui.ScrollingUtil
import com.intellij.ui.components.JBList
import com.intellij.util.text.DateFormatUtil
import com.intellij.util.ui.*
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import java.awt.Component
import java.awt.FlowLayout
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

/**
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/pullrequest/ui/GithubPullRequestsList.kt
 * @author JetBrains s.r.o.
 */
internal class GiteePullRequestsList(private val copyPasteManager: CopyPasteManager,
                                     avatarIconsProviderFactory: CachingGiteeAvatarIconsProvider.Factory,
                                     model: ListModel<GiteePullRequest>)
  : JBList<GiteePullRequest>(model), CopyProvider, DataProvider, Disposable {

  private val avatarIconSize = JBValue.UIInteger("Gitee.PullRequests.List.Assignee.Avatar.Size", 20)
  private val avatarIconsProvider = avatarIconsProviderFactory.create(avatarIconSize, this)

  init {
    selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
    addMouseListener(RightClickSelectionListener())

    val renderer = PullRequestsListCellRenderer()
    cellRenderer = renderer
    UIUtil.putClientProperty(this, UIUtil.NOT_IN_HIERARCHY_COMPONENTS, listOf(renderer))

    ScrollingUtil.installActions(this)
    Disposer.register(this, avatarIconsProvider)
  }

  override fun getToolTipText(event: MouseEvent): String? {
    val childComponent = ListUtil.getDeepestRendererChildComponentAt(this, event.point)
    if (childComponent !is JComponent) return null
    return childComponent.toolTipText
  }

  override fun performCopy(dataContext: DataContext) {
    if (selectedIndex < 0) return
    val selection = model.getElementAt(selectedIndex)
    copyPasteManager.setContents(StringSelection("#${selection.number} ${selection.title}"))
  }

  override fun isCopyEnabled(dataContext: DataContext) = !isSelectionEmpty

  override fun isCopyVisible(dataContext: DataContext) = false

  override fun getData(dataId: String) = if (PlatformDataKeys.COPY_PROVIDER.`is`(dataId)) this else null

  override fun dispose() {}

  private inner class PullRequestsListCellRenderer : ListCellRenderer<GiteePullRequest>, JPanel() {

    private val stateIcon = JLabel()
    private val title = JLabel()
    private val info = JLabel()
    private val labels = JPanel(FlowLayout(FlowLayout.LEFT, 4, 0))
    private val assignees = JPanel().apply {
      layout = BoxLayout(this, BoxLayout.X_AXIS)
    }

    init {
      border = JBUI.Borders.empty(5, 8)

      layout = MigLayout(LC().gridGap("0", "0")
                           .insets("0", "0", "0", "0")
                           .fillX())

      add(stateIcon, CC()
        .gapAfter("${JBUI.scale(5)}px"))
      add(title, CC()
        .minWidth("0px"))
      add(labels, CC()
        .growX()
        .pushX())
      add(assignees, CC()
        .spanY(2)
        .wrap())
      add(info, CC()
        .minWidth("0px")
        .skip(1)
        .spanX(2))
    }

    override fun getListCellRendererComponent(list: JList<out GiteePullRequest>,
                                              value: GiteePullRequest,
                                              index: Int,
                                              isSelected: Boolean,
                                              cellHasFocus: Boolean): Component {
      UIUtil.setBackgroundRecursively(this, GiteeUIUtil.List.WithTallRow.background(list, isSelected))
      val primaryTextColor = GiteeUIUtil.List.WithTallRow.foreground(list, isSelected)
      val secondaryTextColor = GiteeUIUtil.List.WithTallRow.secondaryForeground(list, isSelected)

      stateIcon.apply {
        icon = if (value.state == GiteeIssueState.open) GiteeIcons.PullRequestOpen else GiteeIcons.PullRequestClosed
      }
      title.apply {
        text = value.title
        foreground = primaryTextColor
      }

      info.apply {
        text = "#${value.number} ${value.user.login} on ${DateFormatUtil.formatDate(value.createdAt)}"
        foreground = secondaryTextColor
      }

      labels.apply {
        removeAll()
        for (label in value.labels) add(GiteeUIUtil.createIssueLabelLabel(label))
      }
      assignees.apply {
        removeAll()
        for (assignee in value.assignees) {
          if (componentCount != 0) {
            add(Box.createRigidArea(JBDimension(UIUtil.DEFAULT_HGAP, 0)))
          }
          add(JLabel().apply {
            icon = assignee.let { avatarIconsProvider.getIcon(it) }
            toolTipText = assignee.login
          })
        }
      }

      return this
    }
  }

  private inner class RightClickSelectionListener : MouseAdapter() {
    override fun mousePressed(e: MouseEvent) {
      if (JBSwingUtilities.isRightMouseButton(e)) {
        val row = locationToIndex(e.point)
        if (row != -1) selectionModel.setSelectionInterval(row, row)
      }
    }
  }
}