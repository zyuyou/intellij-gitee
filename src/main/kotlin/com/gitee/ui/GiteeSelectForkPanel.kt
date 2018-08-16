package com.gitee.ui

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.text.NaturalComparator
import com.intellij.ui.SortedComboBoxModel
import javax.swing.JPanel

/**
 * Created by zyuyou on 2018/8/16.
 *
 */
class GiteeSelectForkPanel() {
  private val model: SortedComboBoxModel<String> = SortedComboBoxModel(NaturalComparator.INSTANCE)
  private val comboBox = ComboBox<String>(model)

  val panel: JPanel by lazy {
    com.intellij.ui.layout.panel {
      row("Fork owner:") {
        comboBox(growX)
      }
    }
  }

  fun setUsers(users: Collection<String>) {
    model.clear()
    model.addAll(users)

    if (users.isNotEmpty()) {
      comboBox.selectedIndex = 0
    }
  }

  fun getUser(): String {
    return comboBox.selectedItem!!.toString()
  }

}