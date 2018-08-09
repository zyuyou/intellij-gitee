// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.ui

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CollectionComboBoxModel
import java.awt.event.ItemEvent

class GiteeAccountCombobox(accounts: Set<com.gitee.authentication.accounts.GiteeAccount>,
                           defaultAccount: com.gitee.authentication.accounts.GiteeAccount?,
                           onChange: ((com.gitee.authentication.accounts.GiteeAccount) -> Unit)? = null) : ComboBox<com.gitee.authentication.accounts.GiteeAccount>() {

  init {
    val accountList = accounts.toList()

    model = CollectionComboBoxModel(accountList)

    if (defaultAccount != null) {
      selectedItem = defaultAccount
    } else {
      selectedIndex = 0
    }

    if (onChange != null) addItemListener { if (it.stateChange == ItemEvent.SELECTED) onChange(model.selectedItem as com.gitee.authentication.accounts.GiteeAccount) }

    isEnabled = accounts.size > 1
  }
}
