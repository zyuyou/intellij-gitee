// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest

import com.gitee.pullrequest.config.GiteePullRequestsProjectUISettings
import com.gitee.util.CollectionDelta
import com.gitee.util.GitRemoteUrlCoordinates
import com.gitee.util.GiteeGitHelper
import com.intellij.dvcs.repo.VcsRepositoryMappingListener
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.ui.ChangesViewContentManager
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryChangeListener
import org.jetbrains.annotations.CalledInAwt
import kotlin.properties.Delegates.observable

@Service
internal class GiteePRToolWindowTabsManager(private val project: Project) {
  private val gitHelper = GiteeGitHelper.getInstance()
  private val settings = GiteePullRequestsProjectUISettings.getInstance(project)

  private val contentManager by lazy(LazyThreadSafetyMode.NONE) {
    GiteePRToolWindowsTabsContentManager(project, ChangesViewContentManager.getInstance(project))
  }

  private var remoteUrls by observable(setOf<GitRemoteUrlCoordinates>()) { _, oldValue, newValue ->
    val delta = CollectionDelta(oldValue, newValue)
    for (item in delta.removedItems) {
      contentManager.removeTab(item)
    }
    for (item in delta.newItems) {
      contentManager.addTab(item, Disposable {
        //means that tab closed by user
        if (gitHelper.getPossibleRemoteUrlCoordinates(project).contains(item)) settings.addHiddenUrl(item.url)
        ApplicationManager.getApplication().invokeLater(::updateRemoteUrls) { project.isDisposedOrDisposeInProgress }
      })
    }
  }

  @CalledInAwt
  fun showTab(remoteUrl: GitRemoteUrlCoordinates) {
    settings.removeHiddenUrl(remoteUrl.url)
    updateRemoteUrls()

    contentManager.focusTab(remoteUrl)
  }

  private fun updateRemoteUrls() {
    remoteUrls = gitHelper.getPossibleRemoteUrlCoordinates(project).filter {
      !settings.getHiddenUrls().contains(it.url)
    }.toSet()
  }

  class RemoteUrlsListener(private val project: Project)
    : VcsRepositoryMappingListener, GitRepositoryChangeListener {

    override fun mappingChanged() = updateRemotes()
    override fun repositoryChanged(repository: GitRepository) = updateRemotes()

    private fun updateRemotes() {
      val application = ApplicationManager.getApplication()
      if (application.isDispatchThread) {
        project.service<GiteePRToolWindowTabsManager>().updateRemoteUrls()
      }
      else {
        application.invokeLater(::updateRemotes) { project.isDisposedOrDisposeInProgress }
      }
    }
  }
}
