// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.ui

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
import javax.swing.JComponent
import javax.swing.JList
import javax.swing.JTextArea

class GiteeChooseAccountDialog(project: Project?, parentComponent: JComponent?,
                               accounts: Collection<com.gitee.authentication.accounts.GiteeAccount>,
                               descriptionText: String?, showHosts: Boolean, allowDefault: Boolean,
                               title: String, okText: String)
  : DialogWrapper(project, parentComponent, false, IdeModalityType.PROJECT) {

  private val description: JTextArea? = descriptionText?.let {
    JTextArea().apply {
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
  private val accountsList: JBList<com.gitee.authentication.accounts.GiteeAccount> = JBList<com.gitee.authentication.accounts.GiteeAccount>(accounts).apply {
    cellRenderer = object : ColoredListCellRenderer<com.gitee.authentication.accounts.GiteeAccount>() {
      override fun customizeCellRenderer(list: JList<out com.gitee.authentication.accounts.GiteeAccount>,
                                         value: com.gitee.authentication.accounts.GiteeAccount,
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
    accountsList.selectedIndex = 0
  }

  override fun doValidate(): ValidationInfo? {
    return if (accountsList.selectedValue == null) ValidationInfo("Account is not selected", accountsList) else null
  }

  val account: com.gitee.authentication.accounts.GiteeAccount
    get() = accountsList.selectedValue

  val setDefault: Boolean
    get() = setDefaultCheckBox?.isSelected ?: false

  override fun createCenterPanel(): JComponent? {
    return JBUI.Panels.simplePanel(UIUtil.DEFAULT_HGAP, UIUtil.DEFAULT_VGAP)
      .apply { description?.run(::addToTop) }
      .addToCenter(JBScrollPane(accountsList).apply { preferredSize = JBDimension(150, 80) })
      .apply { setDefaultCheckBox?.run(::addToBottom) }
  }

  override fun getPreferredFocusedComponent() = accountsList
}