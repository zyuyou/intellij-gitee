// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.extensions

import com.gitee.util.GiteeUtil
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import git4idea.remote.GitRepositoryHostingService
import git4idea.remote.InteractiveGitHttpAuthDataProvider
import kotlinx.coroutines.runBlocking

internal class GERepositoryHostingService : GitRepositoryHostingService() {
  override fun getServiceDisplayName(): String = GiteeUtil.SERVICE_DISPLAY_NAME

  @RequiresBackgroundThread
  override fun getInteractiveAuthDataProvider(project: Project, url: String)
    : InteractiveGitHttpAuthDataProvider? = runBlocking {
    GEHttpAuthDataProvider.getAccountsWithTokens(project, url).takeIf { it.isNotEmpty() }?.let {
      GESelectAccountHttpAuthDataProvider(project, it)
    }
  }

  @RequiresBackgroundThread
  override fun getInteractiveAuthDataProvider(project: Project, url: String, login: String)
    : InteractiveGitHttpAuthDataProvider? = runBlocking {
    GEHttpAuthDataProvider.getAccountsWithTokens(project, url).mapNotNull { (acc, credentials) ->
      if (credentials == null) return@mapNotNull null
      val details = GEHttpAuthDataProvider.getAccountDetails(acc, credentials) ?: return@mapNotNull null
      if (details.login != login) return@mapNotNull null
      acc to credentials
    }.takeIf { it.isNotEmpty() }?.let {
      GESelectAccountHttpAuthDataProvider(project, it.toMap())
    }
  }
}