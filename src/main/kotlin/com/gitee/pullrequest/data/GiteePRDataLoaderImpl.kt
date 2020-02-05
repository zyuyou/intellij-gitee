// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeRepositoryPath
import com.gitee.api.GiteeServerPath
import com.google.common.cache.CacheBuilder
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.util.EventDispatcher
import git4idea.commands.Git
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import org.jetbrains.annotations.CalledInAwt
import java.util.*

internal class GiteePRDataLoaderImpl(private val project: Project,
                                     private val progressManager: ProgressManager,
                                     private val git: Git,
                                     private val requestExecutor: GiteeApiRequestExecutor,
                                     private val repository: GitRepository,
                                     private val remote: GitRemote,
                                     private val serverPath: GiteeServerPath,
                                     private val repoPath: GiteeRepositoryPath) : GiteePRDataLoader {

  private var isDisposed = false
  private val cache = CacheBuilder.newBuilder()
    .removalListener<Long, GiteePRDataProviderImpl> {
      runInEdt { invalidationEventDispatcher.multicaster.providerChanged(it.key) }
    }
    .maximumSize(5)
    .build<Long, GiteePRDataProviderImpl>()

  private val invalidationEventDispatcher = EventDispatcher.create(DataInvalidatedListener::class.java)

  init {
    requestExecutor.addListener(this) { invalidateAllData() }
  }

  @CalledInAwt
  override fun invalidateAllData() {
    cache.invalidateAll()
  }

  @CalledInAwt
  override fun getDataProvider(number: Long): GiteePRDataProvider {
    if (isDisposed) throw IllegalStateException("Already disposed")

    return cache.get(number) {
      GiteePRDataProviderImpl(project, progressManager, git, requestExecutor, repository, remote, serverPath,
                                        repoPath.owner, repoPath.repository, number)
    }
  }

  @CalledInAwt
  override fun findDataProvider(number: Long): GiteePRDataProvider? = cache.getIfPresent(number)

  override fun addInvalidationListener(disposable: Disposable, listener: (Long) -> Unit) =
    invalidationEventDispatcher.addListener(object : DataInvalidatedListener {
      override fun providerChanged(pullRequestNumber: Long) {
        listener(pullRequestNumber)
      }
    }, disposable)

  override fun dispose() {
    invalidateAllData()
    isDisposed = true
  }

  private interface DataInvalidatedListener : EventListener {
    fun providerChanged(pullRequestNumber: Long)
  }
}