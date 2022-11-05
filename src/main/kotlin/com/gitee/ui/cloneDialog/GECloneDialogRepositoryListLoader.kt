package com.gitee.ui.cloneDialog

import com.gitee.authentication.accounts.GiteeAccount
import com.intellij.ui.SingleSelectionModel
import javax.swing.ListModel

interface GECloneDialogRepositoryListLoader {
  val loading: Boolean
  val listModel: ListModel<GERepositoryListItem>
  val listSelectionModel: SingleSelectionModel

  fun loadRepositories(account: GiteeAccount)
  fun clear(account: GiteeAccount)
  fun addLoadingStateListener(listener: () -> Unit)
}