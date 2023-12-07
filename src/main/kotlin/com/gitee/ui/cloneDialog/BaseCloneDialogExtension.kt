// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.ui.cloneDialog

import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.accounts.isGEAccount
import com.gitee.icons.GiteeIcons
import com.intellij.collaboration.messages.CollaborationToolsBundle
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtension
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionStatusLine
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionStatusLine.Companion.greyText
import javax.swing.Icon

private val GiteeAccount.nameWithServer: String
  get() {
    val serverPrefix = if (isGEAccount) "" else "${server.host}/"
    return serverPrefix + name
  }

abstract class BaseCloneDialogExtension : VcsCloneDialogExtension {
  override fun getIcon(): Icon = GiteeIcons.Gitee_icon

  protected abstract fun getAccounts(): Collection<GiteeAccount>

  override fun getAdditionalStatusLines(): List<VcsCloneDialogExtensionStatusLine> {
    val accounts = getAccounts()
    if (accounts.isEmpty()) return listOf(greyText(CollaborationToolsBundle.message("clone.dialog.label.no.accounts")))

    return accounts.map { greyText(it.nameWithServer) }
  }
}