// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data.provider

import com.gitee.api.data.GECommit
import com.gitee.pullrequest.data.GEPRIdentifier
import com.gitee.pullrequest.data.service.GEPRChangesService
import com.gitee.util.LazyCancellableBackgroundProcessValue
import com.google.common.graph.Traverser
import com.intellij.openapi.Disposable
import java.util.concurrent.CompletableFuture

class GEPRChangesDataProviderImpl(private val changesService: GEPRChangesService,
                                  private val pullRequestId: GEPRIdentifier,
                                  private val detailsData: GEPRDetailsDataProviderImpl
)
  : GEPRChangesDataProvider, Disposable {

  private var lastKnownBaseSha: String? = null
  private var lastKnownHeadSha: String? = null

  init {
    detailsData.addDetailsLoadedListener(this) {
      val details = detailsData.loadedDetails ?: return@addDetailsLoadedListener

      if (lastKnownBaseSha != null && lastKnownBaseSha != details.baseRefOid &&
          lastKnownHeadSha != null && lastKnownHeadSha != details.headRefOid) {
        reloadChanges()
      }
      lastKnownBaseSha = details.baseRefOid
      lastKnownHeadSha = details.headRefOid
    }
  }

  private val baseBranchFetchRequestValue = LazyCancellableBackgroundProcessValue.create { indicator ->
    detailsData.loadDetails().thenCompose {
      changesService.fetchBranch(indicator, it.baseRefName)
    }
  }

  private val headBranchFetchRequestValue = LazyCancellableBackgroundProcessValue.create {
    changesService.fetch(it, "refs/pull/${pullRequestId.number}/head:")
  }

  private val apiCommitsRequestValue = LazyCancellableBackgroundProcessValue.create {
    changesService.loadCommitsFromApi(it, pullRequestId)
  }

  private val changesProviderValue = LazyCancellableBackgroundProcessValue.create { indicator ->
    val commitsRequest = apiCommitsRequestValue.value

    detailsData.loadDetails()
      .thenCompose {
        changesService.loadMergeBaseOid(indicator, it.baseRefOid, it.headRefOid)
      }.thenCompose { mergeBase ->
        commitsRequest.thenCompose {
          changesService.createChangesProvider(indicator, mergeBase, it)
        }
      }
  }

  override fun loadChanges() = changesProviderValue.value

  override fun reloadChanges() {
    baseBranchFetchRequestValue.drop()
    headBranchFetchRequestValue.drop()
    apiCommitsRequestValue.drop()
    changesProviderValue.drop()
  }

  override fun addChangesListener(disposable: Disposable, listener: () -> Unit) =
    changesProviderValue.addDropEventListener(disposable, listener)

  override fun loadCommitsFromApi(): CompletableFuture<List<GECommit>> = apiCommitsRequestValue.value.thenApply {
    val (lastCommit, graph) = it
    Traverser.forGraph(graph).depthFirstPostOrder(lastCommit).toList()
  }

  override fun addCommitsListener(disposable: Disposable, listener: () -> Unit) =
    apiCommitsRequestValue.addDropEventListener(disposable, listener)

  override fun fetchBaseBranch() = baseBranchFetchRequestValue.value

  override fun fetchHeadBranch() = headBranchFetchRequestValue.value

  override fun dispose() {}
}