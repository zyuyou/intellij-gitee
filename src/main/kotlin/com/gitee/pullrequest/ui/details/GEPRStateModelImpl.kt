// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.details

import com.gitee.api.data.GiteePullRequest
import com.gitee.i18n.GiteeBundle
import com.gitee.pullrequest.action.ui.GiteeMergeCommitMessageDialog
import com.gitee.pullrequest.data.GEPRMergeabilityState
import com.gitee.pullrequest.data.provider.GEPRChangesDataProvider
import com.gitee.pullrequest.data.provider.GEPRStateDataProvider
import com.gitee.util.DelayedTaskScheduler
import com.gitee.util.GiteeUtil.Delegates.observableField
import com.intellij.collaboration.async.CompletableFutureUtil
import com.intellij.collaboration.async.CompletableFutureUtil.handleOnEdt
import com.intellij.collaboration.async.CompletableFutureUtil.successOnEdt
import com.intellij.collaboration.ui.SimpleEventListener
import com.intellij.collaboration.ui.SingleValueModel
import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.EventDispatcher
import java.util.concurrent.CompletableFuture

class GEPRStateModelImpl(private val project: Project,
                         private val stateData: GEPRStateDataProvider,
                         private val changesData: GEPRChangesDataProvider,
                         private val detailsModel: SingleValueModel<out GiteePullRequest>,
                         disposable: Disposable) : GEPRStateModel {

  private val mergeabilityEventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)
  private val busyEventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)
  private val actionErrorEventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)

  private val mergeabilityPoller = DelayedTaskScheduler(3, disposable) {
    reloadMergeabilityState()
  }

  private val details: GiteePullRequest
    get() = detailsModel.value

  override val viewerDidAuthor = details.viewerDidAuthor()
  override val isDraft: Boolean
    get() = details.isDraft

  override fun addAndInvokeDraftStateListener(listener: () -> Unit) {
    var lastIsDraft = isDraft
    detailsModel.addListener {
      if (lastIsDraft != isDraft) listener()
      lastIsDraft = isDraft
    }
    listener()
  }

  override var mergeabilityState: GEPRMergeabilityState? = null
    private set
  override var mergeabilityLoadingError: Throwable? = null
    private set

  override var isBusy: Boolean by observableField(false, busyEventDispatcher)
  override var actionError: Throwable? by observableField(null, actionErrorEventDispatcher)

  init {
    stateData.loadMergeabilityState(disposable) {
      it.handleOnEdt { result: GEPRMergeabilityState?, error: Throwable? ->
        mergeabilityState = result
        mergeabilityLoadingError = error
        mergeabilityEventDispatcher.multicaster.eventOccurred()

        if (error == null && result?.hasConflicts == null) {
          mergeabilityPoller.start()
        }
        else mergeabilityPoller.stop()
      }
    }
  }

  override fun reloadMergeabilityState() {
    stateData.reloadMergeabilityState()
  }

  override fun submitCloseTask() = submitTask {
    stateData.close(EmptyProgressIndicator())
  }

  override fun submitReopenTask() = submitTask {
    stateData.reopen(EmptyProgressIndicator())
  }

  override fun submitMarkReadyForReviewTask() {
    stateData.markReadyForReview(EmptyProgressIndicator())
  }

  override fun submitMergeTask() = submitTask {
    val mergeability = mergeabilityState ?: return@submitTask null
    val dialog = GiteeMergeCommitMessageDialog(project,
                                                GiteeBundle.message("pull.request.merge.message.dialog.title"),
                                                GiteeBundle.message("pull.request.merge.pull.request", details.number),
                                                details.title)
    if (!dialog.showAndGet()) {
      return@submitTask null
    }

    stateData.merge(EmptyProgressIndicator(), dialog.message, mergeability.headRefOid)
  }

  override fun submitRebaseMergeTask() = submitTask {
    val mergeability = mergeabilityState ?: return@submitTask null
    stateData.rebaseMerge(EmptyProgressIndicator(), mergeability.headRefOid)
  }

  override fun submitSquashMergeTask() = submitTask {
    val mergeability = mergeabilityState ?: return@submitTask null
    changesData.loadCommitsFromApi().successOnEdt { commits ->
      val body = "* " + StringUtil.join(commits, { it.messageHeadline }, "\n\n* ")
      val dialog = GiteeMergeCommitMessageDialog(project,
                                                  GiteeBundle.message("pull.request.merge.message.dialog.title"),
                                                  GiteeBundle.message("pull.request.merge.pull.request", details.number),
                                                  body)
      if (!dialog.showAndGet()) {
        throw ProcessCanceledException()
      }
      dialog.message
    }.thenCompose { message ->
      stateData.squashMerge(EmptyProgressIndicator(), message, mergeability.headRefOid)
    }
  }

  private fun submitTask(request: () -> CompletableFuture<*>?) {
    if (isBusy) return
    isBusy = true
    actionError = null

    val task = request()?.handleOnEdt { _, error ->
      actionError = error?.takeIf { !CompletableFutureUtil.isCancellation(it) }
      isBusy = false
    }
    if (task == null) {
      isBusy = false
      return
    }
  }

  override fun addAndInvokeMergeabilityStateLoadingResultListener(listener: () -> Unit) =
    SimpleEventListener.addAndInvokeListener(mergeabilityEventDispatcher, listener)

  override fun addAndInvokeBusyStateChangedListener(listener: () -> Unit) =
    SimpleEventListener.addAndInvokeListener(busyEventDispatcher, listener)

  override fun addAndInvokeActionErrorChangedListener(listener: () -> Unit) =
    SimpleEventListener.addAndInvokeListener(actionErrorEventDispatcher, listener)
}