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