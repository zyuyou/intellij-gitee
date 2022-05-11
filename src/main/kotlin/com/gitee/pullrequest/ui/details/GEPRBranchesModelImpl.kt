// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.details

import com.gitee.api.data.GiteePullRequestDetailed
import com.gitee.pullrequest.data.provider.GEPRDetailsDataProvider
import com.gitee.util.GiteeGitHelper
import com.intellij.collaboration.ui.SimpleEventListener
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.util.EventDispatcher
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.vcs.log.data.DataPackChangeListener
import com.intellij.vcs.log.impl.VcsProjectLog
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository

internal class GEPRBranchesModelImpl(private val valueModel: SingleValueModel<GiteePullRequestDetailed>,
                                     detailsDataProvider: GEPRDetailsDataProvider,
                                     override val localRepository: GitRepository,
                                     private val parentDisposable: Disposable) : GEPRBranchesModel {

  private val changeEventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)

  init {
    VcsProjectLog.runWhenLogIsReady(localRepository.project) {
      if (!Disposer.isDisposed(parentDisposable)) {
        val dataPackListener = DataPackChangeListener {
          notifyChanged()
          detailsDataProvider.reloadDetails()
        }

        it.dataManager.addDataPackChangeListener(dataPackListener)
        Disposer.register(parentDisposable, Disposable {
          it.dataManager.removeDataPackChangeListener(dataPackListener)
        })
      }
    }
  }

  @RequiresEdt
  override fun addAndInvokeChangeListener(listener: () -> Unit) =
    SimpleEventListener.addAndInvokeListener(changeEventDispatcher, parentDisposable, listener)

  override val baseBranch: String
    get() = valueModel.value.baseRefName
  override val headBranch: String
    get() {
      with(valueModel.value) {
        if (headRepository == null) return headRefName
        if (headRepository.isFork || baseRefName == headRefName) {
          return headRepository.owner.login + ":" + headRefName
        }
        else {
          return headRefName
        }
      }
    }

  override val prRemote: GitRemote?
    get() = determinePrRemote()

  override val localBranch: String?
    get() = determineLocalBranch()

  private val headRefName: String
    get() = valueModel.value.headRefName

  private fun notifyChanged() {
    changeEventDispatcher.multicaster.eventOccurred()
  }

  private val url: String?
    get() = valueModel.value.headRepository?.url

  private val sshUrl: String?
    get() = valueModel.value.headRepository?.sshUrl

  private val isFork: Boolean
    get() = valueModel.value.headRepository?.isFork ?: false

  private fun determinePrRemote(): GitRemote? = GiteeGitHelper.getInstance().findRemote(localRepository, url, sshUrl)

  private fun determineLocalBranch(): String? {
    val prRemote = prRemote ?: return null
    return GiteeGitHelper.getInstance().findLocalBranch(localRepository, prRemote, isFork, headRefName)
  }
}
