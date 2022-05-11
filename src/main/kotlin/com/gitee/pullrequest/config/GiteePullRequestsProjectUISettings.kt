// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.config

import com.gitee.api.GERepositoryCoordinates
import com.gitee.api.GERepositoryPath
import com.gitee.api.GiteeServerPath
import com.gitee.authentication.accounts.GEAccountSerializer
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.util.GEGitRepositoryMapping
import com.gitee.util.GEProjectRepositoriesManager
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@Service
@State(name = "GiteePullRequestsUISettings", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)], reportStatistic = false)
class GiteePullRequestsProjectUISettings(private val project: Project)
  : PersistentStateComponentWithModificationTracker<GiteePullRequestsProjectUISettings.SettingsState> {

  private var state: SettingsState = SettingsState()

  class SettingsState : BaseState() {
    var selectedUrlAndAccountId by property<UrlAndAccount?>(null) { it == null }
    var recentSearchFilters by list<String>()
    var recentNewPullRequestHead by property<RepoCoordinatesHolder?>(null) { it == null }
  }

  var selectedRepoAndAccount: Pair<GEGitRepositoryMapping, GiteeAccount>?
    get() {
      val (url, accountId) = state.selectedUrlAndAccountId ?: return null
      val repo = project.service<GEProjectRepositoriesManager>().knownRepositories.find {
        it.gitRemoteUrlCoordinates.url == url
      } ?: return null
      val account = GEAccountSerializer.deserialize(accountId) ?: return null
      return repo to account
    }
    set(value) {
      state.selectedUrlAndAccountId = value?.let { (repo, account) ->
        UrlAndAccount(repo.gitRemoteUrlCoordinates.url, GEAccountSerializer.serialize(account))
      }
    }

  fun getRecentSearchFilters(): List<String> = state.recentSearchFilters.toList()

  fun addRecentSearchFilter(searchFilter: String) {
    val addExisting = state.recentSearchFilters.remove(searchFilter)
    state.recentSearchFilters.add(0, searchFilter)

    if (state.recentSearchFilters.size > RECENT_SEARCH_FILTERS_LIMIT) {
      state.recentSearchFilters.removeLastOrNull()
    }

    if (!addExisting) {
      state.intIncrementModificationCount()
    }
  }

  var recentNewPullRequestHead: GERepositoryCoordinates?
    get() = state.recentNewPullRequestHead?.let { GERepositoryCoordinates(it.server, GERepositoryPath(it.owner, it.repository)) }
    set(value) {
      state.recentNewPullRequestHead = value?.let { RepoCoordinatesHolder(it) }
    }

  override fun getStateModificationCount() = state.modificationCount
  override fun getState() = state
  override fun loadState(state: SettingsState) {
    this.state = state
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project) = project.service<GiteePullRequestsProjectUISettings>()

    private const val RECENT_SEARCH_FILTERS_LIMIT = 10

    class UrlAndAccount private constructor() {

      var url: String = ""
      var accountId: String = ""

      constructor(url: String, accountId: String) : this() {
        this.url = url
        this.accountId = accountId
      }

      operator fun component1() = url
      operator fun component2() = accountId
    }

    class RepoCoordinatesHolder private constructor() {

      var server: GiteeServerPath = GiteeServerPath.DEFAULT_SERVER
      var owner: String = ""
      var repository: String = ""

      constructor(coordinates: GERepositoryCoordinates): this() {
        server = coordinates.serverPath
        owner = coordinates.repositoryPath.owner
        repository = coordinates.repositoryPath.repository
      }
    }
  }
}