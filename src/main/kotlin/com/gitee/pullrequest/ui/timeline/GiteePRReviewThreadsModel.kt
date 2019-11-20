// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.timeline

import com.gitee.api.data.pullrequest.GEPullRequestReviewThread
import com.gitee.pullrequest.comment.ui.GiteePRReviewThreadModel
import com.intellij.ui.CollectionListModel
import com.intellij.util.containers.SortedList

class GiteePRReviewThreadsModel(list: List<GEPullRequestReviewThread>)
  : CollectionListModel<GiteePRReviewThreadModel>(createSortedList(list), true) {

  fun update(list: List<GEPullRequestReviewThread>) {
    val threadsById = list.associateBy { it.id }.toMutableMap()
    val removals = mutableListOf<GiteePRReviewThreadModel>()
    for (item in items) {
      val thread = threadsById.remove(item.id)
      if (thread != null) item.update(thread)
      else removals.add(item)
    }
    for (model in removals) {
      remove(model)
    }
    for (thread in threadsById.values) {
      add(GiteePRReviewThreadModel(thread))
    }
  }

  companion object {
    private fun createSortedList(list: List<GEPullRequestReviewThread>): SortedList<GiteePRReviewThreadModel> {
      val sorted = SortedList<GiteePRReviewThreadModel>(compareBy { it.createdAt })
      for (thread in list) {
        sorted.add(GiteePRReviewThreadModel(thread))
      }
      return sorted
    }
  }
}