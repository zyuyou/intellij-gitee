// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest

import com.gitee.api.GiteeApiRequestExecutorManager
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.AccountTokenChangedListener
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.accounts.GiteeAccountManager
import com.gitee.authentication.ui.GiteeChooseAccountDialog
import com.gitee.ui.util.DisposingWrapper
import com.gitee.util.GitRemoteUrlCoordinates
import com.gitee.util.GiteeUIUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.UIUtil

internal class GiteePRAccountsComponent(private val authManager: GiteeAuthenticationManager,
                                     private val project: Project,
                                     private val remoteUrl: GitRemoteUrlCoordinates,
                                     parentDisposable: Disposable)
  : DisposingWrapper(parentDisposable) {

  private val requestExecutorManager by lazy(LazyThreadSafetyMode.NONE) { GiteeApiRequestExecutorManager.getInstance() }
  private var selectedAccount: GiteeAccount? = null

  init {
    background = UIUtil.getListBackground()

    ApplicationManager.getApplication().messageBus.connect(parentDisposable)
      .subscribe(GiteeAccountManager.ACCOUNT_TOKEN_CHANGED_TOPIC,
        object : AccountTokenChangedListener {
          override fun tokenChanged(account: GiteeAccount) {
            update()
          }
        })
    //TODO: listen to default account changes?
    update()
  }

  private fun update() {
    if (selectedAccount != null) return

    val accounts = authManager.getAccounts().filter { it.server.matches(remoteUrl.url) }

    if (accounts.size == 1) {
      setActualContent(accounts.single())
      return
    }

    val defaultAccount = accounts.find { it == authManager.getDefaultAccount(project) }
    if (defaultAccount != null) {
      setActualContent(defaultAccount)
      return
    }

    if (accounts.isNotEmpty()) {
      showChooseAccountPanel(accounts)
    }
    else {
      showLoginPanel()
    }
  }

  private fun showLoginPanel() {
    setCenteredContent(GiteeUIUtil.createNoteWithAction(::requestNewAccount).apply {
      append("Log in", SimpleTextAttributes.LINK_ATTRIBUTES, Runnable { requestNewAccount() })
      append(" to GitHub to view pull requests", SimpleTextAttributes.GRAYED_ATTRIBUTES)
    })
  }

  private fun requestNewAccount() {
    authManager.requestNewAccount(project)
    IdeFocusManager.getInstance(project).requestFocusInProject(this@GiteePRAccountsComponent, project)
  }

  private fun showChooseAccountPanel(accounts: List<GiteeAccount>) {
    setCenteredContent(GiteeUIUtil.createNoteWithAction { chooseAccount(accounts) }.apply {
      append("Select", SimpleTextAttributes.LINK_ATTRIBUTES, Runnable { chooseAccount(accounts) })
      append(" GitHub account to view pull requests", SimpleTextAttributes.GRAYED_ATTRIBUTES)
    })
  }

  private fun chooseAccount(accounts: List<GiteeAccount>) {
    val dialog = GiteeChooseAccountDialog(project, null, accounts, null, true, true)
    if (dialog.showAndGet()) {
      setActualContent(dialog.account)
      IdeFocusManager.getInstance(project).requestFocusInProject(this@GiteePRAccountsComponent, project)
    }
  }

  private fun setActualContent(account: GiteeAccount) {
    selectedAccount = account
    val disposable = Disposer.newDisposable()
    setContent(GiteePRRequestExecutorComponent(requestExecutorManager, project, remoteUrl, account, disposable), disposable)
  }
}