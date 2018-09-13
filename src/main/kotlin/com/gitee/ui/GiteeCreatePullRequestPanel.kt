/*
 * Copyright 2016-2018 码云 - Gitee
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gitee.ui

import com.gitee.api.GiteeFullPath
import com.gitee.util.GiteeCreatePullRequestWorker.BranchInfo
import com.gitee.util.GiteeCreatePullRequestWorker.ForkInfo
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.SortedComboBoxModel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBDimension
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.event.DocumentEvent

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/ui/GithubCreatePullRequestPanel.java
 * @author JetBrains s.r.o.
 */
internal class GiteeCreatePullRequestPanel {
  private val titleTextField: JTextField = JTextField().apply {
    preferredSize = JBDimension(150, -1)
  }

  private val descriptionTextArea: JTextArea = JTextArea()

  private val branchModel = SortedComboBoxModel<BranchInfo> { o1, o2 -> StringUtil.naturalCompare(o1.remoteName, o2.remoteName) }
  val branchComboBox = ComboBox<BranchInfo>(branchModel)

  private val forkModel = SortedComboBoxModel<ForkInfo> { o1, o2 -> StringUtil.naturalCompare(o1.path.user, o2.path.user) }
  val forkComboBox = ComboBox<ForkInfo>(forkModel)

  val showDiffButton = JButton("Show Diff")
  val selectForkButton = JButton("Select Other Fork")

  private var titleDescriptionUserModified = false

  val panel: JPanel by lazy {
    com.intellij.ui.layout.panel {
      row("Base fork:") {
        cell {
          forkComboBox(growX, pushX)
        }
        cell {
          selectForkButton(growX)
        }
      }
      row("Base branch:") {
        cell {
          branchComboBox(growX, pushX)
        }
        cell {
          showDiffButton(growX)
        }
      }
      row("Title: ") {
        titleTextField()
      }
      row("Description:") {
      }
      row {
        JBScrollPane(descriptionTextArea).apply {
          minimumSize = JBDimension(150, 60)
          border = EmptyBorder(0, 0, 0, 0)
        }()
      }
    }
  }

  init {
    descriptionTextArea.border = BorderFactory.createEtchedBorder()

    val userModifiedDocumentListener = object : DocumentAdapter() {
      override fun textChanged(e: DocumentEvent) {
        titleDescriptionUserModified = true
      }
    }

    descriptionTextArea.document.addDocumentListener(userModifiedDocumentListener)
    titleTextField.document.addDocumentListener(userModifiedDocumentListener)
  }

  fun getTitle(): String {
    return titleTextField.text
  }

  fun getDescription(): String {
    return descriptionTextArea.text
  }

  fun getSelectedFork(): ForkInfo? {
    return forkModel.selectedItem
  }

  fun getSelectedBranch(): BranchInfo? {
    return branchModel.selectedItem
  }

  fun setSelectedFork(path: GiteeFullPath?) {
    if (path != null) {
      for (info in forkModel.items) {
        if (path == info.path) {
          forkModel.selectedItem = info
          return
        }
      }
    }

    if (forkModel.size > 0) forkModel.selectedItem = forkModel.get(0)
  }

  fun setSelectedBranch(branch: String?) {
    if (branch != null) {
      for (info in branchModel.items) {
        if (branch == info.remoteName) {
          branchModel.selectedItem = info
          return
        }
      }
    }

    if (branchModel.size > 0) branchModel.selectedItem = branchModel.get(0)
  }

  fun setForks(forks: Collection<ForkInfo>) {
    forkModel.selectedItem = null
    forkModel.setAll(forks)
  }

  fun setBranches(branches: Collection<BranchInfo>) {
    branchModel.selectedItem = null
    branchModel.setAll(branches)
  }

  fun setTitle(title: String?) {
    titleTextField.text = title
    titleDescriptionUserModified = false
  }

  fun setDescription(title: String?) {
    descriptionTextArea.text = title
    titleDescriptionUserModified = false
  }

  fun isTitleDescriptionEmptyOrNotModified(): Boolean {
    return !titleDescriptionUserModified
      || StringUtil.isEmptyOrSpaces(titleTextField.text)
      && StringUtil.isEmptyOrSpaces(descriptionTextArea.text)
  }

  fun setDiffEnabled(enabled: Boolean) {
    showDiffButton.isEnabled = enabled
  }

  fun getTitleTextField(): JComponent {
    return titleTextField
  }

  fun getPreferredComponent(): JComponent {
    return titleTextField
  }
}