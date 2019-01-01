/*
 *  Copyright 2016-2019 码云 - Gitee
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.gitee.pullrequest

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.data.GiteeRepoDetailed
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.pullrequest.action.GiteePullRequestKeys
import com.gitee.pullrequest.avatars.CachingGiteeAvatarIconsProvider
import com.gitee.pullrequest.data.GiteePullRequestsDataLoader
import com.gitee.pullrequest.data.GiteePullRequestsLoader
import com.gitee.pullrequest.ui.*
import com.gitee.util.CachingGiteeUserAvatarLoader
import com.gitee.util.GiteeImageResizer
import com.intellij.codeInsight.AutoPopupController
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.OnePixelSplitter
import git4idea.commands.Git
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import org.jetbrains.annotations.CalledInAwt
import javax.swing.JComponent


/**
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/pullrequest/GithubPullRequestsComponentFactory.kt
 * @author JetBrains s.r.o.
 */
internal class GiteePullRequestsComponentFactory(private val project: Project,
                                                 private val copyPasteManager: CopyPasteManager,
                                                 private val progressManager: ProgressManager,
                                                 private val git: Git,
                                                 private val avatarLoader: CachingGiteeUserAvatarLoader,
                                                 private val imageResizer: GiteeImageResizer,
                                                 private val actionManager: ActionManager,
                                                 private val autoPopupController: AutoPopupController) {

  fun createComponent(requestExecutor: GiteeApiRequestExecutor,
                      repository: GitRepository, remote: GitRemote,
                      repoDetails: GiteeRepoDetailed,
                      account: GiteeAccount): JComponent? {

    val avatarIconsProviderFactory = CachingGiteeAvatarIconsProvider.Factory(avatarLoader, imageResizer, requestExecutor)

    return GiteePullRequestsComponent(requestExecutor, avatarIconsProviderFactory, repository, remote, repoDetails, account)
  }

  inner class GiteePullRequestsComponent(private val requestExecutor: GiteeApiRequestExecutor,
                                          avatarIconsProviderFactory: CachingGiteeAvatarIconsProvider.Factory,
                                          private val repository: GitRepository, private val remote: GitRemote,
                                          private val repoDetails: GiteeRepoDetailed,
                                          private val account: GiteeAccount)
    : OnePixelSplitter("Gitee.PullRequests.Component", 0.33f), Disposable, DataProvider {

    private val dataLoader = GiteePullRequestsDataLoader(project, progressManager, git, requestExecutor, repository, remote)

    private val changes = GiteePullRequestChangesComponent(project).apply {
      diffAction.registerCustomShortcutSet(this@GiteePullRequestsComponent, this@GiteePullRequestsComponent)
    }

    private val details = GiteePullRequestDetailsComponent(avatarIconsProviderFactory)
    private val preview = GiteePullRequestPreviewComponent(changes, details)

    private val listLoader = GiteePullRequestsLoader(progressManager, requestExecutor, account.server, repoDetails.fullPath)

    private val list = GiteePullRequestsListComponent(project, copyPasteManager, actionManager, autoPopupController,
                                                       listLoader,
                                                       avatarIconsProviderFactory).apply {
      requestExecutor.addListener(this) { this.refresh() }
    }


    init {
      firstComponent = list
      secondComponent = preview
      isFocusCycleRoot = true

      list.selectionModel.addChangesListener(object : GiteePullRequestsListSelectionModel.SelectionChangedListener {
        override fun selectionChanged() {
          val dataProvider = list.selectionModel.current?.let(dataLoader::getDataProvider)
          preview.setPreviewDataProvider(dataProvider)
        }
      }, preview)

      dataLoader.addProviderChangesListener(object : GiteePullRequestsDataLoader.ProviderChangedListener {
        override fun providerChanged(pullRequestNumber: Long) {
          runInEdt {
            if (Disposer.isDisposed(preview)) return@runInEdt
            val selection = list.selectionModel.current
            if (selection != null && selection.number == pullRequestNumber) {
              preview.setPreviewDataProvider(dataLoader.getDataProvider(selection))
            }
          }
        }
      }, preview)
    }

    @CalledInAwt
    fun refreshAllPullRequests() {
      list.refresh()
      dataLoader.invalidateAllData()
    }

    //TODO: refresh in list
    @CalledInAwt
    fun refreshPullRequest(number: Long) {
      dataLoader.invalidateData(number)
    }

    override fun getData(dataId: String): Any? {
      if (Disposer.isDisposed(this)) return null
      return when {
        GiteePullRequestKeys.REPOSITORY.`is`(dataId) -> repository
        GiteePullRequestKeys.REMOTE.`is`(dataId) -> remote
        GiteePullRequestKeys.REPO_DETAILS.`is`(dataId) -> repoDetails
        GiteePullRequestKeys.SERVER_PATH.`is`(dataId) -> account.server
        GiteePullRequestKeys.API_REQUEST_EXECUTOR.`is`(dataId) -> requestExecutor
        GiteePullRequestKeys.PULL_REQUESTS_COMPONENT.`is`(dataId) -> this
        GiteePullRequestKeys.SELECTED_PULL_REQUEST.`is`(dataId) -> list.selectionModel.current
        GiteePullRequestKeys.SELECTED_PULL_REQUEST_DATA_PROVIDER.`is`(dataId) ->
          list.selectionModel.current?.let(dataLoader::getDataProvider)
        else -> null
      }
    }

    override fun dispose() {
      Disposer.dispose(list)
      Disposer.dispose(preview)
      Disposer.dispose(changes)
      Disposer.dispose(details)

      Disposer.dispose(listLoader)
      Disposer.dispose(dataLoader)
    }
  }
}