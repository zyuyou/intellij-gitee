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
package com.gitee.authentication.ui

import com.gitee.authentication.accounts.GiteeAccount
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CollectionComboBoxModel
import java.awt.event.ItemEvent

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/authentication/ui/GithubAccountCombobox.kt
 * @author JetBrains s.r.o.
 */
class GiteeAccountCombobox(accounts: Set<GiteeAccount>,
                           defaultAccount: GiteeAccount?,
                           onChange: ((GiteeAccount) -> Unit)? = null) : ComboBox<GiteeAccount>() {

  init {
    val accountList = accounts.toList()

    model = CollectionComboBoxModel(accountList)

    if (defaultAccount != null) {
      selectedItem = defaultAccount
    } else {
      selectedIndex = 0
    }

    if (onChange != null) addItemListener { if (it.stateChange == ItemEvent.SELECTED) onChange(model.selectedItem as GiteeAccount) }

    isEnabled = accounts.size > 1
  }
}
