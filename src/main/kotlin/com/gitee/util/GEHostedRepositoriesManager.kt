// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.gitee.util

import com.gitee.api.GiteeServerPath
import com.gitee.authentication.accounts.GEAccountManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import git4idea.remote.hosting.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.future.await
import org.jetbrains.annotations.VisibleForTesting

@Service(Service.Level.PROJECT)
class GEHostedRepositoriesManager(project: Project, cs: CoroutineScope) : HostedGitRepositoriesManager<GEGitRepositoryMapping>{

  @VisibleForTesting
  internal val knownRepositoriesFlow = run {
    val gitRemotesFlow = gitRemotesFlow(project).distinctUntilChanged()

    val accountsServersFlow = service<GEAccountManager>().accountsState.map { accounts ->
      mutableSetOf(GiteeServerPath.DEFAULT_SERVER) + accounts.map { it.server }
    }.distinctUntilChanged()

    val discoveredServersFlow = gitRemotesFlow.discoverServers(accountsServersFlow) { remote ->
      GitHostingUrlUtil.findServerAt(LOG, remote) {
        val server = GiteeServerPath.from(it.toString())
        val metadata = service<GEEnterpriseServerMetadataLoader>().loadMetadata(server).await()
        if (metadata != null) server else null
      }
    }.runningFold(emptySet<GiteeServerPath>()) { accumulator, value ->
      accumulator + value
    }.distinctUntilChanged()

    val serversFlow = accountsServersFlow.combine(discoveredServersFlow) { servers1, servers2 ->
      servers1 + servers2
    }

    gitRemotesFlow.mapToServers(serversFlow) { server, remote ->
      GEGitRepositoryMapping.create(server, remote)
    }.onEach {
      LOG.debug("New list of known repos: $it")
    }
  }

  override val knownRepositoriesState: StateFlow<Set<GEGitRepositoryMapping>> =
//    knownRepositoriesFlow.stateIn(cs, getStateSharingStartConfig(), emptySet())
    knownRepositoriesFlow.stateIn(cs, SharingStarted.Eagerly, emptySet())

  companion object {
    private val LOG = logger<GEHostedRepositoriesManager>()

//    private fun getStateSharingStartConfig() =
//      if (ApplicationManager.getApplication().isUnitTestMode) SharingStarted.Eagerly else SharingStarted.Lazily
  }
}