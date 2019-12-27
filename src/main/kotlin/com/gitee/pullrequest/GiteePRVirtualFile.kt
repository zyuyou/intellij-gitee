// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest

import com.gitee.api.data.GiteePullRequest
import com.gitee.pullrequest.action.GiteePRActionDataContext
import com.gitee.pullrequest.data.GiteePullRequestDataProvider
import com.intellij.testFramework.LightVirtualFile

internal class GiteePRVirtualFile(val context: GiteePRActionDataContext,
                                  val pullRequest: GiteePullRequest,
                                  val dataProvider: GiteePullRequestDataProvider)
  : LightVirtualFile(pullRequest.title, GiteePRFileType.INSTANCE, "") {

  init {
    isWritable = false
  }

  override fun getPath(): String = pullRequest.url

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is GiteePRVirtualFile) return false

    if (pullRequest != other.pullRequest) return false

    return true
  }

  override fun hashCode(): Int {
    return pullRequest.hashCode()
  }
}