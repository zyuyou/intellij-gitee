// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data.provider

import com.gitee.pullrequest.data.GEPRIdentifier
import com.gitee.pullrequest.data.GEPRMergeabilityState
import com.gitee.pullrequest.data.service.GEPRStateService
import com.gitee.util.LazyCancellableBackgroundProcessValue
import com.intellij.collaboration.async.CompletableFutureUtil.completionOnEdt
import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.messages.MessageBus
import java.util.concurrent.CompletableFuture

class GEPRStateDataProviderImpl(private val stateService: GEPRStateService,
                                private val pullRequestId: GEPRIdentifier,
                                private val messageBus: MessageBus,
                                private val detailsData: GEPRDetailsDataProvider)
  : GEPRStateDataProvider, Disposable {

  private var lastKnownBaseBranch: String? = null
  private var lastKnownBaseSha: String? = null
  private var lastKnownHeadSha: String? = null

  init {
    detailsData.addDetailsLoadedListener(this) {
      val details = detailsData.loadedDetails ?: return@addDetailsLoadedListener

      if (lastKnownBaseBranch != null && lastKnownBaseBranch != details.baseRefName) {
        baseBranchProtectionRulesRequestValue.drop()
        reloadMergeabilityState()
      }
      lastKnownBaseBranch = details.baseRefName


      if (lastKnownBaseSha != null && lastKnownBaseSha != details.baseRefOid &&
          lastKnownHeadSha != null && lastKnownHeadSha != details.headRefOid) {
        reloadMergeabilityState()
      }
      lastKnownBaseSha = details.baseRefOid
      lastKnownHeadSha = details.headRefOid
    }
  }

  private val baseBranchProtectionRulesRequestValue = LazyCancellableBackgroundProcessValue.create { indicator ->
    detailsData.loadDetails().thenCompose {
      stateService.loadBranchProtectionRules(indicator, pullRequestId, it.baseRefName)
    }
  }
  private val mergeabilityStateRequestValue = LazyCancellableBackgroundProcessValue.create { indicator ->
    val baseBranchProtectionRulesRequest = baseBranchProtectionRulesRequestValue.value
    detailsData.loadDetails().thenCompose { details ->

      baseBranchProtectionRulesRequest.thenCompose {
        stateService.loadMergeabilityState(indicator, pullRequestId, details.headRefOid, details.url, it)
      }
    }
  }

  override fun loadMergeabilityState(): CompletableFuture<GEPRMergeabilityState> = mergeabilityStateRequestValue.value

  override fun reloadMergeabilityState() {
    if (baseBranchProtectionRulesRequestValue.lastLoadedValue == null)
      baseBranchProtectionRulesRequestValue.drop()
    mergeabilityStateRequestValue.drop()
  }

  override fun addMergeabilityStateListener(disposable: Disposable, listener: () -> Unit) =
    mergeabilityStateRequestValue.addDropEventListener(disposable, listener)

  override fun close(progressIndicator: ProgressIndicator): CompletableFuture<Unit> =
    stateService.close(progressIndicator, pullRequestId).notifyState()

  override fun reopen(progressIndicator: ProgressIndicator): CompletableFuture<Unit> =
    stateService.reopen(progressIndicator, pullRequestId).notifyState()

  override fun markReadyForReview(progressIndicator: ProgressIndicator): CompletableFuture<Unit> =
    stateService.markReadyForReview(progressIndicator, pullRequestId).notifyState().completionOnEdt {
      mergeabilityStateRequestValue.drop()
    }

  override fun merge(progressIndicator: ProgressIndicator, commitMessage: Pair<String, String>, currentHeadRef: String)
    : CompletableFuture<Unit> = stateService.merge(progressIndicator, pullRequestId, commitMessage, currentHeadRef).notifyState()

  override fun rebaseMerge(progressIndicator: ProgressIndicator, currentHeadRef: String): CompletableFuture<Unit> =
    stateService.rebaseMerge(progressIndicator, pullRequestId, currentHeadRef).notifyState()

  override fun squashMerge(progressIndicator: ProgressIndicator, commitMessage: Pair<String, String>, currentHeadRef: String)
    : CompletableFuture<Unit> = stateService.squashMerge(progressIndicator, pullRequestId, commitMessage, currentHeadRef).notifyState()

  private fun <T> CompletableFuture<T>.notifyState(): CompletableFuture<T> =
    completionOnEdt {
      detailsData.reloadDetails()
      messageBus.syncPublisher(GEPRDataOperationsListener.TOPIC).onStateChanged()
    }

  override fun dispose() {
    mergeabilityStateRequestValue.drop()
    baseBranchProtectionRulesRequestValue.drop()
  }
}