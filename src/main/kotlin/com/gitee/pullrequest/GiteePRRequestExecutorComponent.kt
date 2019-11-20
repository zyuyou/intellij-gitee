// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequestExecutorManager
import com.gitee.authentication.accounts.AccountTokenChangedListener
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.accounts.GiteeAccountManager
import com.gitee.ui.util.DisposingWrapper
import com.gitee.util.GitRemoteUrlCoordinates
import com.gitee.util.GiteeUIUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.UIUtil

class GiteePRRequestExecutorComponent(private val requestExecutorManager: GiteeApiRequestExecutorManager,
                                   private val project: Project,
                                   private val remoteUrl: GitRemoteUrlCoordinates,
                                   val account: GiteeAccount,
                                   parentDisposable: Disposable)
  : DisposingWrapper(parentDisposable) {

  private val componentFactory by lazy(LazyThreadSafetyMode.NONE) { project.service<GiteePRComponentFactory>() }

  private var requestExecutor: GiteeApiRequestExecutor? = null

  init {
    background = UIUtil.getListBackground()

    ApplicationManager.getApplication().messageBus.connect(parentDisposable)
      .subscribe(GiteeAccountManager.ACCOUNT_TOKEN_CHANGED_TOPIC,
        object : AccountTokenChangedListener {
          override fun tokenChanged(account: GiteeAccount) {
            update()
          }
        })
    update()
  }

  private fun update() {
    if (requestExecutor != null) return

    try {
      val executor = requestExecutorManager.getExecutor(account)
      setActualContent(executor)
    }
    catch (e: Exception) {
      setCenteredContent(GiteeUIUtil.createNoteWithAction(::createRequestExecutorWithUserInput).apply {
        append("Log in", SimpleTextAttributes.LINK_ATTRIBUTES, Runnable { createRequestExecutorWithUserInput() })
        append(" to GitHub to view pull requests", SimpleTextAttributes.GRAYED_ATTRIBUTES)
      })
    }
  }

  private fun createRequestExecutorWithUserInput() {
    requestExecutorManager.getExecutor(account, project)
    IdeFocusManager.getInstance(project).requestFocusInProject(this@GiteePRRequestExecutorComponent, project)
  }

  private fun setActualContent(executor: GiteeApiRequestExecutor.WithTokensAuth) {
    requestExecutor = executor
    val disposable = Disposer.newDisposable()
    setContent(componentFactory.createComponent(remoteUrl, account, executor, disposable), disposable)
  }
}