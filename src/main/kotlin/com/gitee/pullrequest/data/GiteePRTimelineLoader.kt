// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeGQLRequests
import com.gitee.api.GiteeRepositoryPath
import com.gitee.api.GiteeServerPath
import com.gitee.api.data.pullrequest.timeline.GiteePRTimelineItem
import com.gitee.api.util.SimpleGiteeGQLPagesLoader
import com.gitee.pullrequest.ui.timeline.GiteePRTimelineMergingModel
import com.intellij.openapi.progress.ProgressManager

class GiteePRTimelineLoader(progressManager: ProgressManager,
                            requestExecutor: GiteeApiRequestExecutor,
                            serverPath: GiteeServerPath,
                            repoPath: GiteeRepositoryPath,
                            number: Long,
                            private val listModel: GiteePRTimelineMergingModel)
  : GiteeGQLPagedListLoader<GiteePRTimelineItem>(progressManager,
                                           SimpleGiteeGQLPagesLoader(requestExecutor, { p ->
                                             GiteeGQLRequests.PullRequest.Timeline.items(serverPath, repoPath.owner, repoPath.repository,
                                                                                      number, p)
                                           })) {
  override val hasLoadedItems: Boolean
    get() = listModel.size != 0

  override fun handleResult(list: List<GiteePRTimelineItem>) {
    listModel.add(list.filter { it !is GiteePRTimelineItem.Unknown })
  }

  override fun reset() {
    super.reset()
    listModel.removeAll()
    loadMore()
  }
}
