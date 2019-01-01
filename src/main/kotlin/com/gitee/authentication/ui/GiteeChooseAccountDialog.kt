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
package com.gitee.authentication.ui

import com.gitee.authentication.accounts.GiteeAccount
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Component
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JTextArea
import javax.swing.ListSelectionModel

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/authentication/ui/GithubChooseAccountDialog.kt
 * @author JetBrains s.r.o.
 */
class GiteeChooseAccountDialog(project: Project?, parentComponent: Component?,
                               accounts: Collection<GiteeAccount>,
                               descriptionText: String?, showHosts: Boolean, allowDefault: Boolean,
                               title: String = "Choose Gitee Account", okText: String = "Choose")
  : DialogWrapper(project, parentComponent, false, IdeModalityType.PROJECT) {

  private val description: JTextArea? = descriptionText?.let {
    JTextArea().apply {
      minimumSize = Dimension(0, 0)
      font = UIUtil.getLabelFont()
      text = it
      lineWrap = true
      wrapStyleWord = true
      isEditable = false
      isFocusable = false
      isOpaque = false
      border = null
      margin = JBUI.emptyInsets()
    }
  }
  private val accountsList: JBList<GiteeAccount> = JBList<GiteeAccount>(accounts).apply {
    selectionMode = ListSelectionModel.SINGLE_SELECTION

    cellRenderer = object : ColoredListCellRenderer<GiteeAccount>() {
      override fun customizeCellRenderer(list: JList<out GiteeAccount>,
                                         value: GiteeAccount,
                                         index: Int,
                                         selected: Boolean,
                                         hasFocus: Boolean) {
        append(value.name)
        if (showHosts) {
          append(" ")
          append(value.server.toString(), SimpleTextAttributes.GRAYED_ATTRIBUTES)
        }
        border = JBUI.Borders.empty(0, UIUtil.DEFAULT_HGAP)
      }
    }
  }
  private val setDefaultCheckBox: JBCheckBox? = if (allowDefault) JBCheckBox("Set as default account for current project") else null

  init {
    this.title = title
    setOKButtonText(okText)
    init()
    pack()
    accountsList.selectedIndex = 0
  }

  override fun getDimensionServiceKey() = "Gitee.Dialog.Accounts.Choose"

  override fun doValidate(): ValidationInfo? {
    return if (accountsList.selectedValue == null) ValidationInfo("Account is not selected", accountsList) else null
  }

  val account: GiteeAccount
    get() = accountsList.selectedValue

  val setDefault: Boolean
    get() = setDefaultCheckBox?.isSelected ?: false

  override fun createCenterPanel(): JComponent? {
    return JBUI.Panels.simplePanel(UIUtil.DEFAULT_HGAP, UIUtil.DEFAULT_VGAP)
      .apply { description?.run(::addToTop) }
      .addToCenter(JBScrollPane(accountsList).apply {
        preferredSize = JBDimension(150, 20 * (accountsList.itemsCount + 1))
      })
      .apply { setDefaultCheckBox?.run(::addToBottom) }
  }

  override fun getPreferredFocusedComponent() = accountsList
}