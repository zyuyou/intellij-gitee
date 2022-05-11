// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.util

import com.gitee.api.GiteeServerPath
import com.gitee.authentication.accounts.GEAccountManager
import com.gitee.authentication.accounts.GiteeAccount
import com.intellij.collaboration.async.CompletableFutureUtil.errorOnEdt
import com.intellij.collaboration.async.CompletableFutureUtil.successOnEdt
import com.intellij.collaboration.auth.AccountsListener
import com.intellij.collaboration.hosting.GitHostingUrlUtil
import com.intellij.dvcs.repo.VcsRepositoryMappingListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.util.SingleAlarm
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.intellij.util.messages.Topic
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryChangeListener
import git4idea.repo.GitRepositoryManager
import org.jetbrains.annotations.CalledInAny
import kotlin.properties.Delegates

@Service
class GEProjectRepositoriesManager(private val project: Project) : Disposable {
  private val updateAlarm = SingleAlarm(task = ::doUpdateRepositories, delay = 50, parentDisposable = this)

  private val accountManager: GEAccountManager
    get() = service()

  var knownRepositories by Delegates.observable(emptySet<GEGitRepositoryMapping>()) { _, oldValue, newValue ->
    if (oldValue != newValue) {
      ApplicationManager.getApplication().messageBus.syncPublisher(LIST_CHANGES_TOPIC)
        .repositoryListChanged(newValue, project)
    }
  }

  private val serversFromDiscovery = HashSet<GiteeServerPath>()

  init {
    accountManager.addListener(this, object : AccountsListener<GiteeAccount> {
      override fun onAccountListChanged(old: Collection<GiteeAccount>, new: Collection<GiteeAccount>) = runInEdt {
        updateRepositories()
      }
    })
    updateRepositories()
  }

  fun findKnownRepositories(repository: GitRepository): List<GEGitRepositoryMapping> {
    return knownRepositories.filter {
      it.gitRemoteUrlCoordinates.repository == repository
    }
  }

  @CalledInAny
  private fun updateRepositories() {
    if (ApplicationManager.getApplication().isUnitTestMode) {
      doUpdateRepositories()
    }
    else {
      updateAlarm.request()
    }
  }

  //TODO: execute on pooled thread - need to make GiteeAccountManager ready
  @RequiresEdt
  private fun doUpdateRepositories() {
    LOG.debug("Repository list update started")
    val gitRepositories = project.service<GitRepositoryManager>().repositories
    if (gitRepositories.isEmpty()) {
      knownRepositories = emptySet()
      LOG.debug("No repositories found")
      return
    }

    val remotes = gitRepositories.flatMap { repo ->
      repo.remotes.flatMap { remote ->
        remote.urls.mapNotNull { url ->
          GitRemoteUrlCoordinates(url, remote, repo)
        }
      }
    }
    LOG.debug("Found remotes: $remotes")

    val authenticatedServers = accountManager.accounts.map { it.server }
    val servers = mutableListOf<GiteeServerPath>().apply {
      add(GiteeServerPath.DEFAULT_SERVER)
      addAll(authenticatedServers)
      addAll(serversFromDiscovery)
    }

    val repositories = HashSet<GEGitRepositoryMapping>()
    for (remote in remotes) {
      val repository = servers.find { it.matches(remote.url) }?.let { GEGitRepositoryMapping.create(it, remote) }
      if (repository != null)
        repositories.add(repository)
      else {
        scheduleEnterpriseServerDiscovery(remote)
      }
    }
    LOG.debug("New list of known repos: $repositories")
    knownRepositories = repositories

    for (server in authenticatedServers) {
      if (server.isGiteeDotCom()) continue

      service<GEEnterpriseServerMetadataLoader>().loadMetadata(server).successOnEdt {
//        GEPRStatisticsCollector.logEnterpriseServerMeta(project, server, it)
      }
    }
  }

  @RequiresEdt
  private fun scheduleEnterpriseServerDiscovery(remote: GitRemoteUrlCoordinates) {
    val uri = GitHostingUrlUtil.getUriFromRemoteUrl(remote.url)
    LOG.debug("Extracted URI $uri from remote ${remote.url}")
    if (uri == null) return

    val host = uri.host ?: return
    val path = uri.path ?: return
    val pathParts = path.removePrefix("/").split('/').takeIf { it.size >= 2 } ?: return
    val serverSuffix = if (pathParts.size == 2) null else pathParts.subList(0, pathParts.size - 2).joinToString("/", "/")

    val server = GiteeServerPath(false, host, null, serverSuffix)
    val serverHttp = GiteeServerPath(true, host, null, serverSuffix)
    val server8080 = GiteeServerPath(true, host, 8080, serverSuffix)
    LOG.debug("Scheduling GEE server discovery for $server, $serverHttp and $server8080")

    val serverManager = service<GEEnterpriseServerMetadataLoader>()
    serverManager.loadMetadata(server).successOnEdt {
      LOG.debug("Found GEE server at $server")
      serversFromDiscovery.add(server)
      invokeLater(runnable = ::doUpdateRepositories)
    }.errorOnEdt {
      serverManager.loadMetadata(serverHttp).successOnEdt {
        LOG.debug("Found GEE server at $serverHttp")
        serversFromDiscovery.add(serverHttp)
        invokeLater(runnable = ::doUpdateRepositories)
      }.errorOnEdt {
        serverManager.loadMetadata(server8080).successOnEdt {
          LOG.debug("Found GEE server at $server8080")
          serversFromDiscovery.add(server8080)
          invokeLater(runnable = ::doUpdateRepositories)
        }
      }
    }
  }

  fun addRepositoryListChangedListener(disposable: Disposable, listener: () -> Unit) {
    ApplicationManager.getApplication().messageBus.connect(disposable).subscribe(LIST_CHANGES_TOPIC, object : ListChangeListener {
      override fun repositoryListChanged(newList: Set<GEGitRepositoryMapping>, project: Project) = listener()
    })
  }

  class RemoteUrlsListener(private val project: Project) : VcsRepositoryMappingListener, GitRepositoryChangeListener {
    override fun mappingChanged() = runInEdt(project) { updateRepositories(project) }
    override fun repositoryChanged(repository: GitRepository) = runInEdt(project) { updateRepositories(project) }
  }

  interface ListChangeListener {
    fun repositoryListChanged(newList: Set<GEGitRepositoryMapping>, project: Project)
  }

  companion object {
    private val LOG = logger<GEProjectRepositoriesManager>()

    @JvmField
    @Topic.AppLevel
    val LIST_CHANGES_TOPIC = Topic(ListChangeListener::class.java, Topic.BroadcastDirection.NONE)

    private inline fun runInEdt(project: Project, crossinline runnable: () -> Unit) {
      val application = ApplicationManager.getApplication()
      if (application.isDispatchThread) runnable()
      else application.invokeLater({ runnable() }) { project.isDisposed }
    }

    private fun updateRepositories(project: Project) {
      try {
        if (!project.isDisposed) project.service<GEProjectRepositoriesManager>().updateRepositories()
      } catch (e: Exception) {
        LOG.info("Error occurred while updating repositories", e)
      }
    }
  }

  override fun dispose() {}
}