// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeRepositoryCoordinates
import com.gitee.api.data.pullrequest.GEPullRequestShort
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.pullrequest.data.service.GiteePullRequestsMetadataService
import com.gitee.pullrequest.data.service.GiteePullRequestsSecurityService
import com.gitee.pullrequest.data.service.GiteePullRequestsStateService
import com.gitee.pullrequest.search.GiteePullRequestSearchQueryHolder
import com.gitee.util.GitRemoteUrlCoordinates
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.util.messages.MessageBus
import com.intellij.util.messages.Topic
import javax.swing.ListModel

internal class GiteePullRequestsDataContext(val gitRepositoryCoordinates: GitRemoteUrlCoordinates,
                                            val repositoryCoordinates: GiteeRepositoryCoordinates,
                                            val account: GiteeAccount,
                                            val requestExecutor: GiteeApiRequestExecutor,
                                            val messageBus: MessageBus,
                                            val listModel: ListModel<GEPullRequestShort>,
                                            val searchHolder: GiteePullRequestSearchQueryHolder,
                                            val listLoader: GiteePRListLoader,
                                            val dataLoader: GiteePullRequestsDataLoader,
                                            val securityService: GiteePullRequestsSecurityService,
                                            val busyStateTracker: GiteePullRequestsBusyStateTracker, //TODO: move to ui
                                            val metadataService: GiteePullRequestsMetadataService,
                                            val stateService: GiteePullRequestsStateService) : Disposable {

  override fun dispose() {
    Disposer.dispose(messageBus)
    Disposer.dispose(dataLoader)
    Disposer.dispose(listLoader)
    Disposer.dispose(metadataService)
  }

  companion object {
    val PULL_REQUEST_EDITED_TOPIC = Topic(PullRequestEditedListener::class.java)

    interface PullRequestEditedListener {
      fun onPullRequestEdited(number: Long)
    }
  }
}
