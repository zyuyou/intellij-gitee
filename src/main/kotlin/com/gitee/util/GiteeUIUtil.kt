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

package com.gitee.util

import com.gitee.api.data.GiteeIssueLabel
import com.gitee.api.data.GiteeUser
import com.gitee.pullrequest.avatars.CachingGiteeAvatarIconsProvider
import com.intellij.UtilBundle
import com.intellij.openapi.editor.impl.view.FontLayoutService
import com.intellij.openapi.vcs.changes.issueLinks.LinkMouseListenerBase
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.components.JBLabel
import com.intellij.util.text.DateFormatUtil
import com.intellij.util.ui.*
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.Color
import java.awt.Component
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.*

object GiteeUIUtil {
  val avatarSize = JBUI.uiIntValue("Gitee.Avatar.Size", 20)

  fun focusPanel(panel: JComponent) {
    val focusManager = IdeFocusManager.findInstanceByComponent(panel)
    val toFocus = focusManager.getFocusTargetFor(panel) ?: return
    focusManager.doWhenFocusSettlesDown { focusManager.requestFocus(toFocus, true) }
  }

  fun createIssueLabelLabel(label: GiteeIssueLabel): JBLabel = JBLabel(" ${label.name} ", UIUtil.ComponentStyle.SMALL).apply {
    background = getLabelBackground(label)
    foreground = getLabelForeground(background)
  }.andOpaque()

  fun getLabelBackground(label: GiteeIssueLabel): JBColor {
    val apiColor = ColorUtil.fromHex(label.color)
    return JBColor(apiColor, ColorUtil.darker(apiColor, 3))
  }

  fun getLabelForeground(bg: Color): Color = if (ColorUtil.isDark(bg)) Color.white else Color.black

  fun setTransparentRecursively(component: Component) {
    if (component is JComponent) {
      component.isOpaque = false
      for (c in component.components) {
        setTransparentRecursively(c)
      }
    }
  }

  fun getFontEM(component: JComponent): Float {
    val metrics = component.getFontMetrics(component.font)
    //em dash character
    return FontLayoutService.getInstance().charWidth2D(metrics, '\u2014'.toInt())
  }

  fun formatActionDate(date: Date): String {
    val prettyDate = DateFormatUtil.formatPrettyDate(date).toLowerCase()
    val datePrefix = if (prettyDate.equals(UtilBundle.message("date.format.today"), true) ||
        prettyDate.equals(UtilBundle.message("date.format.yesterday"), true)) ""
    else "on "
    return datePrefix + prettyDate
  }

  fun createNoteWithAction(action: () -> Unit): SimpleColoredComponent {
    return SimpleColoredComponent().apply {
      isFocusable = true
      isOpaque = false

      LinkMouseListenerBase.installSingleTagOn(this)
      registerKeyboardAction({ action() },
          KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
          JComponent.WHEN_FOCUSED)
    }
  }

  data class SelectableWrapper<T>(val value: T, var selected: Boolean = false)

  sealed class SelectionListCellRenderer<T> : ListCellRenderer<SelectableWrapper<T>>, BorderLayoutPanel() {

    private val mainLabel = JLabel()
    private val checkIconLabel = JLabel()

    init {
      checkIconLabel.iconTextGap = JBUI.scale(UIUtil.DEFAULT_VGAP)
      checkIconLabel.border = JBUI.Borders.empty(0, 4)

      addToLeft(checkIconLabel)
      addToCenter(mainLabel)

      border = JBUI.Borders.empty(4, 0)
    }

    override fun getListCellRendererComponent(list: JList<out SelectableWrapper<T>>,
                                              value: SelectableWrapper<T>,
                                              index: Int,
                                              isSelected: Boolean,
                                              cellHasFocus: Boolean): Component {
      foreground = UIUtil.getListForeground(isSelected, true)
      background = UIUtil.getListBackground(isSelected, true)

      mainLabel.foreground = foreground
      mainLabel.font = font

      mainLabel.text = getText(value.value)
      mainLabel.icon = getIcon(value.value)

      val icon = LafIconLookup.getIcon("checkmark", isSelected, false)
      checkIconLabel.icon = if (value.selected) icon else EmptyIcon.create(icon)

      return this
    }

    abstract fun getText(value: T): String
    abstract fun getIcon(value: T): Icon

    class Users(private val iconsProvider: CachingGiteeAvatarIconsProvider)
      : SelectionListCellRenderer<GiteeUser>() {
      override fun getText(value: GiteeUser) = value.login
      override fun getIcon(value: GiteeUser) = iconsProvider.getIcon(value.avatarUrl)
    }

    class Labels : SelectionListCellRenderer<GiteeIssueLabel>() {
      override fun getText(value: GiteeIssueLabel) = value.name
      override fun getIcon(value: GiteeIssueLabel) = ColorIcon(16, ColorUtil.fromHex(value.color))
    }
  }
}