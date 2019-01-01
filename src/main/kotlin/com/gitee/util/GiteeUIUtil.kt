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
import com.intellij.ui.ColorUtil
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.UIUtil
import java.awt.Color
import javax.swing.JList

object GiteeUIUtil {
  object List {
    object WithTallRow {
      private val selectionBackground = JBColor(0xE9EEF5, 0x464A4D)
      private val unfocusedSelectionBackground = JBColor(0xF5F5F5, 0x464A4D)

      fun foreground(list: JList<*>, isSelected: Boolean): Color {
        val default = UIUtil.getListForeground()
        return if (isSelected) {
          if (list.hasFocus()) JBColor.namedColor("Gitee.List.tallRow.selectionForeground", default)
          else JBColor.namedColor("Gitee.List.tallRow.selectionForeground.unfocused", default)
        }
        else JBColor.namedColor("Gitee.List.tallRow.foreground", default)
      }

      fun secondaryForeground(list: JList<*>, isSelected: Boolean): Color {
        return if (isSelected) {
          foreground(list, true)
        }
        else JBColor.namedColor("Gitee.List.tallRow.secondary.foreground", UIUtil.getContextHelpForeground())
      }

      fun background(list: JList<*>, isSelected: Boolean): Color {
        return if (isSelected) {
          if (list.hasFocus()) JBColor.namedColor("Gitee.List.tallRow.selectionBackground", selectionBackground)
          else JBColor.namedColor("Gitee.List.tallRow.selectionBackground.unfocused", unfocusedSelectionBackground)
        }
        else list.background
      }
    }
  }

  fun createIssueLabelLabel(label: GiteeIssueLabel): JBLabel = JBLabel(" ${label.name} ", UIUtil.ComponentStyle.MINI).apply {
    val apiColor = ColorUtil.fromHex(label.color)
    background = JBColor(apiColor, ColorUtil.darker(apiColor, 3))
    foreground = computeForeground(background)
  }.andOpaque()

  private fun computeForeground(bg: Color) = if (ColorUtil.isDark(bg)) Color.white else Color.black
}