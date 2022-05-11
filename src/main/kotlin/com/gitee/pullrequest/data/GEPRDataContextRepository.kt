// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data

import com.gitee.api.*
import com.gitee.api.data.GiteeUser
import com.gitee.api.data.pullrequest.GETeam
import com.gitee.api.data.request.search.GiteeIssueSearchType
import com.gitee.api.util.GEApiPagedListLoader
import com.gitee.api.util.GiteeApiSearchQueryBuilder
import com.gitee.api.util.SimpleGEApiPagesLoader
import com.gitee.api.util.SimpleGEGQLPagesLoader
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.accounts.GiteeAccountInformationProvider
import com.gitee.i18n.GiteeBundle
import com.gitee.pullrequest.GEPRDiffRequestModelImpl
import com.gitee.pullrequest.data.service.*
import com.gitee.pullrequest.search.GEPRSearchQueryHolderImpl
import com.gitee.ui.avatars.GEAvatarIconsProvider
import com.gitee.util.CachingGEUserAvatarLoader
import com.gitee.util.GitRemoteUrlCoordinates
import com.gitee.util.GiteeSharedProjectSettings
import com.gitee.util.LazyCancellableBackgroundProcessValue
import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.collaboration.async.CompletableFutureUtil.successOnEdt
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresEdt
import java.io.IOException
import java.util.concurrent.CompletableFuture

@Service
internal class GEPRDataContextRepository(private val project: Project) {

  private val repositories = mutableMapOf<GERepositoryCoordinates, LazyCancellableBackgroundProcessValue<GEPRDataContext>>()

  @RequiresEdt
  fun acquireContext(repository: GERepositoryCoordinates, remote: GitRemoteUrlCoordinates,
                     account: GiteeAccount, requestExecutor: GiteeApiRequestExecutor): CompletableFuture<GEPRDataContext> {

    return repositories.getOrPut(repository) {
      val contextDisposable = Disposer.newDisposable()
      LazyCancellableBackgroundProcessValue.create { indicator ->
        ProgressManager.getInstance().submitIOTask(indicator) {
          try {
            loadContext(indicator, account, requestExecutor, repository, remote)
          }
          catch (e: Exception) {
            if (e !is ProcessCanceledException) LOG.info("Error occurred while creating data context", e)
            throw e
          }
        }.successOnEdt { ctx ->
          if (Disposer.isDisposed(contextDisposable)) {
            Disposer.dispose(ctx)
          }
          else {
            Disposer.register(contextDisposable, ctx)
          }
          ctx
        }
      }.also {
        it.addDropEventListener {
          Disposer.dispose(contextDisposable)
        }
      }
    }.value
  }

  @RequiresEdt
  fun clearContext(repository: GERepositoryCoordinates) {
    repositories.remove(repository)?.drop()
  }

//  @RequiresBackgroundThread
//  @Throws(IOException::class)
//  private fun loadContext(indicator: ProgressIndicator,
//                          account: GiteeAccount,
//                          requestExecutor: GiteeApiRequestExecutor,
//                          parsedRepositoryCoordinates: GERepositoryCoordinates,
//                          remoteCoordinates: GitRemoteUrlCoordinates): GEPRDataContext {
//
//    indicator.text = GiteeBundle.message("pull.request.loading.account.info")
//    val accountDetails = GiteeAccountInformationProvider.getInstance().getInformation(requestExecutor, indicator, account)
//    indicator.checkCanceled()
//
//    indicator.text = GiteeBundle.message("pull.request.loading.repo.info")
//
//    val repositoryInfo =
//      requestExecutor.execute(
//        indicator,
//        GEGQLRequests.Repo.find(GERepositoryCoordinates(account.server, parsedRepositoryCoordinates.repositoryPath))
//      ) ?: throw IllegalArgumentException(
//        "Repository ${parsedRepositoryCoordinates.repositoryPath} does not exist at ${account.server} or you don't have access."
//      )
//
//    val currentUser = GEUser(accountDetails.nodeId, accountDetails.login, accountDetails.htmlUrl, accountDetails.avatarUrl!!, accountDetails.name)
//
//    indicator.text = GiteeBundle.message("pull.request.loading.user.teams.info")
//    val repoOwner = repositoryInfo.owner
//    val currentUserTeams = if (repoOwner is GERepositoryOwnerName.Organization)
//      SimpleGEGQLPagesLoader(requestExecutor, {
//        GEGQLRequests.Organization.Team.findByUserLogins(account.server, repoOwner.login, listOf(currentUser.login), it)
//      }).loadAll(indicator)
//    else emptyList()
//    indicator.checkCanceled()
//
//    // repository might have been renamed/moved
//    val apiRepositoryPath = repositoryInfo.path
//    val apiRepositoryCoordinates = GERepositoryCoordinates(account.server, apiRepositoryPath)
//
//    val securityService = GEPRSecurityServiceImpl(GiteeSharedProjectSettings.getInstance(project),
//                                                  account, currentUser, currentUserTeams,
//                                                  repositoryInfo)
//    val detailsService = GEPRDetailsServiceImpl(ProgressManager.getInstance(), requestExecutor, apiRepositoryCoordinates)
//    val stateService = GEPRStateServiceImpl(ProgressManager.getInstance(), securityService,
//                                            requestExecutor, account.server, apiRepositoryPath)
//    val commentService = GEPRCommentServiceImpl(ProgressManager.getInstance(), requestExecutor, apiRepositoryCoordinates)
//    val changesService = GEPRChangesServiceImpl(ProgressManager.getInstance(), project, requestExecutor,
//                                                remoteCoordinates, apiRepositoryCoordinates)
//    val reviewService = GEPRReviewServiceImpl(ProgressManager.getInstance(), securityService, requestExecutor, apiRepositoryCoordinates)
//    val filesService = GEPRFilesServiceImpl(ProgressManager.getInstance(), requestExecutor, apiRepositoryCoordinates)
//
//    val searchHolder = GEPRSearchQueryHolderImpl().apply {
//      query = GEPRSearchQuery.DEFAULT
//    }
//    val listLoader = GEGQLPagedListLoader(ProgressManager.getInstance(),
//                                          SimpleGEGQLPagesLoader(requestExecutor, { p ->
//                                            GEGQLRequests.PullRequest.search(account.server,
//                                                                             buildQuery(apiRepositoryPath, searchHolder.query),
//                                                                             p)
//                                          }))
//    val listUpdatesChecker = GEPRListETagUpdateChecker(ProgressManager.getInstance(), requestExecutor, account.server, apiRepositoryPath)
//
//    val dataProviderRepository = GEPRDataProviderRepositoryImpl(detailsService, stateService, reviewService, filesService, commentService,
//                                                                changesService) { id ->
//      GEGQLPagedListLoader(ProgressManager.getInstance(),
//                           SimpleGEGQLPagesLoader(requestExecutor, { p ->
//                             GEGQLRequests.PullRequest.Timeline.items(account.server, apiRepositoryPath.owner, apiRepositoryPath.repository,
//                                                                      id.number, p)
//                           }, true)
//      )
//    }
//
//    val repoDataService = GEPRRepositoryDataServiceImpl(ProgressManager.getInstance(), requestExecutor,
//                                                        remoteCoordinates, apiRepositoryCoordinates,
//                                                        repoOwner,
//                                                        repositoryInfo.id, repositoryInfo.defaultBranch, repositoryInfo.isFork)
//
//    val avatarIconsProvider = GEAvatarIconsProvider(CachingGEUserAvatarLoader.getInstance(), requestExecutor)
//
//    val filesManager = GEPRFilesManagerImpl(project, parsedRepositoryCoordinates)
//
//    indicator.checkCanceled()
//    val creationService = GEPRCreationServiceImpl(ProgressManager.getInstance(), requestExecutor, repoDataService)
//    return GEPRDataContext(searchHolder, listLoader, listUpdatesChecker, dataProviderRepository,
//                           securityService, repoDataService, creationService, detailsService, avatarIconsProvider, filesManager,
//                           GEPRDiffRequestModelImpl()
//    )
//  }

  @RequiresBackgroundThread
  @Throws(IOException::class)
  private fun loadContext(indicator: ProgressIndicator,
                          account: GiteeAccount,
                          requestExecutor: GiteeApiRequestExecutor,
                          parsedRepositoryCoordinates: GERepositoryCoordinates,
                          remoteCoordinates: GitRemoteUrlCoordinates): GEPRDataContext {

    indicator.text = GiteeBundle.message("pull.request.loading.account.info")
    val accountDetails = GiteeAccountInformationProvider.getInstance().getInformation(requestExecutor, indicator, account)
    indicator.checkCanceled()

    indicator.text = GiteeBundle.message("pull.request.loading.repo.info")

    val repositoryInfo =
      requestExecutor.execute(
        indicator,
        GiteeApiRequests.Repos.get(GERepositoryCoordinates(account.server, parsedRepositoryCoordinates.repositoryPath))
      ) ?: throw IllegalArgumentException(
        "Repository ${parsedRepositoryCoordinates.repositoryPath} does not exist at ${account.server} or you don't have access."
      )

//    val currentUser = GEUser(accountDetails.nodeId, accountDetails.login, accountDetails.htmlUrl, accountDetails.avatarUrl!!, accountDetails.name)
    val currentUser = accountDetails as GiteeUser

    indicator.text = GiteeBundle.message("pull.request.loading.user.teams.info")
    val repoOwner = repositoryInfo.owner
//    val currentUserTeams =
//      if (repoOwner is GERepositoryOwnerName.Organization)
//        SimpleGEGQLPagesLoader(requestExecutor, {
//          GEGQLRequests.Organization.Team.findByUserLogins(account.server, repoOwner.login, listOf(currentUser.login), it)
//        }).loadAll(indicator)
//      else
//        emptyList()
    val currentUserTeams = emptyList<GETeam>()
    indicator.checkCanceled()

    // repository might have been renamed/moved
    val apiRepositoryPath = repositoryInfo.fullPath
    val apiRepositoryCoordinates = GERepositoryCoordinates(account.server, apiRepositoryPath)


    val securityService = GEPRSecurityServiceImpl(
      GiteeSharedProjectSettings.getInstance(project),
      account, currentUser, currentUserTeams, repositoryInfo
    )
    val detailsService = GEPRDetailsServiceImpl(ProgressManager.getInstance(), requestExecutor, apiRepositoryCoordinates)
    val stateService = GEPRStateServiceImpl(ProgressManager.getInstance(), securityService, requestExecutor, account.server, apiRepositoryPath)
    val commentService = GEPRCommentServiceImpl(ProgressManager.getInstance(), requestExecutor, apiRepositoryCoordinates)
    val changesService = GEPRChangesServiceImpl(ProgressManager.getInstance(), project, requestExecutor, remoteCoordinates, apiRepositoryCoordinates)
    val reviewService = GEPRReviewServiceImpl(ProgressManager.getInstance(), securityService, requestExecutor, apiRepositoryCoordinates)
    val filesService = GEPRFilesServiceImpl(ProgressManager.getInstance(), requestExecutor, apiRepositoryCoordinates)

    val searchHolder = GEPRSearchQueryHolderImpl().apply {
      query = GEPRSearchQuery.DEFAULT
    }
//    val listLoader = GEGQLPagedListLoader(ProgressManager.getInstance(),
//      SimpleGEGQLPagesLoader(requestExecutor, { p ->
//        GEGQLRequests.PullRequest.search(account.server, buildQuery(apiRepositoryPath, searchHolder.query), p)
//      }))
    val listLoader = GEApiPagedListLoader(ProgressManager.getInstance(),
      SimpleGEApiPagesLoader(requestExecutor, { p ->
        GiteeApiRequests.Repos.PullRequests.search(account.server, apiRepositoryPath, searchHolder.query.toString(), p)
      }))
    val listUpdatesChecker = GEPRListETagUpdateChecker(ProgressManager.getInstance(), requestExecutor, account.server, apiRepositoryPath)

    val dataProviderRepository = GEPRDataProviderRepositoryImpl(detailsService, stateService, reviewService, filesService, commentService,
      changesService) { id ->
      GEGQLPagedListLoader(ProgressManager.getInstance(),
        SimpleGEGQLPagesLoader(requestExecutor, { p ->
          GEGQLRequests.PullRequest.Timeline.items(account.server, apiRepositoryPath.owner, apiRepositoryPath.repository,
            id.number, p)
        }, true)
      )
    }

    val repoDataService = GEPRRepositoryDataServiceImpl(ProgressManager.getInstance(), requestExecutor,
      remoteCoordinates, apiRepositoryCoordinates,
      repoOwner,
      repositoryInfo.id, repositoryInfo.defaultBranch, repositoryInfo.isFork)

    val avatarIconsProvider = GEAvatarIconsProvider(CachingGEUserAvatarLoader.getInstance(), requestExecutor)

    val filesManager = GEPRFilesManagerImpl(project, parsedRepositoryCoordinates)

    indicator.checkCanceled()
    val creationService = GEPRCreationServiceImpl(ProgressManager.getInstance(), requestExecutor, repoDataService)
    return GEPRDataContext(searchHolder, listLoader, listUpdatesChecker, dataProviderRepository,
      securityService, repoDataService, creationService, detailsService, avatarIconsProvider, filesManager,
      GEPRDiffRequestModelImpl()
    )
  }

  @RequiresEdt
  fun findContext(repositoryCoordinates: GERepositoryCoordinates): GEPRDataContext? = repositories[repositoryCoordinates]?.lastLoadedValue

  companion object {
    private val LOG = logger<GEPRDataContextRepository>()

    fun getInstance(project: Project) = project.service<GEPRDataContextRepository>()

    private fun buildQuery(repoPath: GERepositoryPath, searchQuery: GEPRSearchQuery?): String {
      return GiteeApiSearchQueryBuilder.searchQuery {
        qualifier("type", GiteeIssueSearchType.pr.name)
        qualifier("repo", repoPath.toString())
        searchQuery?.buildApiSearchQuery(this)
      }
    }
  }
}