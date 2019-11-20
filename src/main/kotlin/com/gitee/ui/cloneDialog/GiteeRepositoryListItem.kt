// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.ui.cloneDialog

import com.gitee.api.data.GiteeRepo
import com.gitee.api.data.GiteeUser
import com.gitee.authentication.accounts.GiteeAccount
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.cloneDialog.SearchableListItem
import javax.swing.JList

sealed class GiteeRepositoryListItem(
    val account: GiteeAccount
) : SearchableListItem {
  override val stringToSearch: String?
    get() = ""

  abstract fun customizeRenderer(renderer: ColoredListCellRenderer<GiteeRepositoryListItem>,
                                 list: JList<out GiteeRepositoryListItem>)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as GiteeRepositoryListItem

    if (account != other.account) return false

    return true
  }

  override fun hashCode(): Int {
    return account.hashCode()
  }

  class Repo(
      account: GiteeAccount,
      val user: GiteeUser,
      val repo: GiteeRepo
  ) : GiteeRepositoryListItem(account) {
    override val stringToSearch get() = repo.fullName

    override fun customizeRenderer(renderer: ColoredListCellRenderer<GiteeRepositoryListItem>,
                                   list: JList<out GiteeRepositoryListItem>): Unit =
        with(renderer) {
          ipad.left = 10
          toolTipText = repo.description
//          append(if (repo.owner.login == user.login) repo.name else repo.fullName)
          append("[ ${repo.namespace.type.lang } ] ${repo.humanName}")
        }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false
      if (!super.equals(other)) return false

      other as Repo

      if (user != other.user) return false
      if (repo != other.repo) return false

      return true
    }

    override fun hashCode(): Int {
      var result = super.hashCode()
      result = 31 * result + user.hashCode()
      result = 31 * result + repo.hashCode()
      return result
    }
  }

  class Error(
      account: GiteeAccount,
      private val errorText: String,
      private val linkText: String,
      private val linkHandler: Runnable
  ) : GiteeRepositoryListItem(account) {

    override fun customizeRenderer(renderer: ColoredListCellRenderer<GiteeRepositoryListItem>,
                                   list: JList<out GiteeRepositoryListItem>) =
        with(renderer) {
          ipad.left = 10
          toolTipText = null
          append(errorText, SimpleTextAttributes.ERROR_ATTRIBUTES)
          append(" ")
          append(linkText, SimpleTextAttributes.LINK_ATTRIBUTES, linkHandler)
        }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false
      if (!super.equals(other)) return false

      other as Error

      if (errorText != other.errorText) return false
      if (linkText != other.linkText) return false
      if (linkHandler != other.linkHandler) return false

      return true
    }

    override fun hashCode(): Int {
      var result = super.hashCode()
      result = 31 * result + errorText.hashCode()
      result = 31 * result + linkText.hashCode()
      result = 31 * result + linkHandler.hashCode()
      return result
    }
  }
}