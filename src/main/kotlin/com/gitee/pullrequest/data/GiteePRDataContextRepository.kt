// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequests
import com.gitee.api.GiteeRepositoryCoordinates
import com.gitee.api.data.GiteePullRequest
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.accounts.GiteeAccountInformationProvider
import com.gitee.pullrequest.data.GiteePRDataContext.Companion.PULL_REQUEST_EDITED_TOPIC
import com.gitee.pullrequest.data.GiteePRDataContext.Companion.PullRequestEditedListener
import com.gitee.pullrequest.data.service.GiteePullRequestsMetadataServiceImpl
import com.gitee.pullrequest.data.service.GiteePullRequestsSecurityServiceImpl
import com.gitee.pullrequest.data.service.GiteePullRequestsStateServiceImpl
import com.gitee.pullrequest.search.GiteePullRequestSearchQueryHolderImpl
import com.gitee.util.GitRemoteUrlCoordinates
import com.gitee.util.GiteeSharedProjectSettings
import com.gitee.util.GiteeUrlUtil
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.ui.CollectionListModel
import com.intellij.util.messages.ListenerDescriptor
import com.intellij.util.messages.MessageBusFactory
import com.intellij.util.messages.MessageBusOwner
import git4idea.commands.Git
import org.jetbrains.annotations.CalledInBackground
import java.io.IOException

@Service
internal class GiteePRDataContextRepository(private val project: Project) {

  @CalledInBackground
  @Throws(IOException::class)
  fun getContext(indicator: ProgressIndicator,
                 account: GiteeAccount, requestExecutor: GiteeApiRequestExecutor,
                 gitRemoteCoordinates: GitRemoteUrlCoordinates): GiteePRDataContext {
    val fullPath = GiteeUrlUtil.getUserAndRepositoryFromRemoteUrl(gitRemoteCoordinates.url)
                   ?: throw IllegalArgumentException(
                     "Invalid Gitee Repository URL - ${gitRemoteCoordinates.url} is not a Gitee repository")

    indicator.text = "Loading account information"
    val accountDetails = GiteeAccountInformationProvider.getInstance().getInformation(requestExecutor, indicator, account)
    indicator.checkCanceled()

    indicator.text = "Loading repository information"
    val repoDetails = requestExecutor.execute(indicator, GiteeApiRequests.Repos.get(account.server, fullPath.owner, fullPath.repository))
                      ?: throw IllegalArgumentException("Repository $fullPath does not exist at ${account.server} or you don't have access.")

    val messageBus = MessageBusFactory.getInstance().createMessageBus(object : MessageBusOwner {
      override fun isDisposed() = project.isDisposed

      override fun createListener(descriptor: ListenerDescriptor) = throw UnsupportedOperationException()
    })

    val listModel = CollectionListModel<GiteePullRequest>()
    val searchHolder = GiteePullRequestSearchQueryHolderImpl()
//    val listLoader = GiteePRListLoaderImpl(ProgressManager.getInstance(), requestExecutor, account.server, repoDetails.fullPath, listModel, searchHolder)
    val listLoader = GiteeFakePRListLoaderImpl(ProgressManager.getInstance(), requestExecutor, account.server, repoDetails.fullPath, listModel, searchHolder)
    val dataLoader = GiteePRDataLoaderImpl(project, ProgressManager.getInstance(), Git.getInstance(), requestExecutor,
                                                      gitRemoteCoordinates.repository, gitRemoteCoordinates.remote,
                                                      account.server, repoDetails.fullPath)

    messageBus.connect().subscribe(PULL_REQUEST_EDITED_TOPIC, object : PullRequestEditedListener {
      override fun onPullRequestEdited(number: Long) {
        runInEdt {
          val dataProvider = dataLoader.findDataProvider(number)
          dataProvider?.reloadDetails()
          dataProvider?.detailsRequest?.let { listLoader.reloadData(it) }
        }
      }
    })
    val securityService = GiteePullRequestsSecurityServiceImpl(GiteeSharedProjectSettings.getInstance(project), accountDetails, repoDetails)
    val busyStateTracker = GiteePRBusyStateTrackerImpl()
    val metadataService = GiteePullRequestsMetadataServiceImpl(ProgressManager.getInstance(), messageBus, requestExecutor, account.server,
                                                                repoDetails.fullPath)
    val stateService = GiteePullRequestsStateServiceImpl(project, ProgressManager.getInstance(), messageBus, dataLoader,
                                                          busyStateTracker,
                                                          requestExecutor, account.server, repoDetails.fullPath)

    return GiteePRDataContext(gitRemoteCoordinates, GiteeRepositoryCoordinates(account.server, repoDetails.fullPath), account,
                                     requestExecutor, messageBus, listModel, searchHolder, listLoader, dataLoader, securityService,
                                     busyStateTracker, metadataService, stateService)
  }

  companion object {
    fun getInstance(project: Project) = project.service<GiteePRDataContextRepository>()
  }
}