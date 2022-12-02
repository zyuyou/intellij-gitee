// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.gitee.util

import com.gitee.api.GiteeServerPath
import com.gitee.authentication.accounts.GEAccountManager
import com.intellij.collaboration.async.disposingScope
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import git4idea.remote.GitRemoteUrlCoordinates
import git4idea.remote.hosting.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.future.await
import org.jetbrains.annotations.VisibleForTesting

@Service
class GEHostedRepositoriesManager(project: Project) : HostedGitRepositoriesManager<GEGitRepositoryMapping>, Disposable {

  @VisibleForTesting
  internal val knownRepositoriesFlow = run {
    val gitRemotesFlow = gitRemotesFlow(project).distinctUntilChanged()

    val accountsServersFlow = service<GEAccountManager>().accountsState.map { accounts ->
      mutableSetOf(GiteeServerPath.DEFAULT_SERVER) + accounts.map { it.server }
    }.distinctUntilChanged()

    val discoveredServersFlow = gitRemotesFlow.discoverServers(accountsServersFlow) {
      checkForDedicatedServer(it)
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
    knownRepositoriesFlow.stateIn(disposingScope(), getStateSharingStartConfig(), emptySet())

  private suspend fun checkForDedicatedServer(remote: GitRemoteUrlCoordinates): GiteeServerPath? {
    val uri = GitHostingUrlUtil.getUriFromRemoteUrl(remote.url)
    LOG.debug("Extracted URI $uri from remote ${remote.url}")
    if (uri == null) return null

    val host = uri.host ?: return null
    val path = uri.path ?: return null
    val pathParts = path.removePrefix("/").split('/').takeIf { it.size >= 2 } ?: return null
    val serverSuffix = if (pathParts.size == 2) null else pathParts.subList(0, pathParts.size - 2).joinToString("/", "/")

    for (server in listOf(
      GiteeServerPath(false, host, null, serverSuffix),
      GiteeServerPath(true, host, null, serverSuffix),
      GiteeServerPath(true, host, 8080, serverSuffix)
    )) {
      LOG.debug("Looking for GEE server at $server")
      try {
        service<GEEnterpriseServerMetadataLoader>().loadMetadata(server).await()
        LOG.debug("Found GEE server at $server")
        return server
      }
      catch (ignored: Throwable) {
      }
    }
    return null
  }

  override fun dispose() = Unit

  companion object {
    private val LOG = logger<GEHostedRepositoriesManager>()

    private fun getStateSharingStartConfig() =
      if (ApplicationManager.getApplication().isUnitTestMode) SharingStarted.Eagerly else SharingStarted.Lazily
  }
}