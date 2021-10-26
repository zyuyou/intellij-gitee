// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.ui

import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.ui.GEAccountsHost.Companion.createAddAccountLink
import com.gitee.i18n.GiteeBundle.message
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.layout.Row
import com.intellij.ui.layout.applyToComponent

internal class GEAccountsComboBoxModel(accounts: Set<GiteeAccount>, selection: GiteeAccount?) :
  CollectionComboBoxModel<GiteeAccount>(accounts.toMutableList(), selection),
  GEAccountsHost {

  override fun addAccount(server: GiteeServerPath, login: String, token: String) {
    val account = GiteeAuthenticationManager.getInstance().registerAccount(login, server, token)

    add(account)
    selectedItem = account
  }

  override fun addAccount(server: GiteeServerPath, login: String, tokens: Pair<String, String>) {
    addAccount(server, login, "${tokens.first}&${tokens.second}")
  }

  override fun isAccountUnique(login: String, server: GiteeServerPath): Boolean =
    GiteeAuthenticationManager.getInstance().isAccountUnique(login, server)

  companion object {
    fun Row.accountSelector(model: CollectionComboBoxModel<GiteeAccount>, onChange: (() -> Unit)? = null) =
      cell {
        comboBox(model, { model.selected }, { })
          .constraints(pushX, growX)
          .withValidationOnApply { if (model.selected == null) error(message("dialog.message.account.cannot.be.empty")) else null }
          .applyToComponent {
            if (onChange != null) addActionListener { onChange() }
          }

        if (model.size == 0) {
          createAddAccountLink()().withLargeLeftGap()
        }
      }
  }
}