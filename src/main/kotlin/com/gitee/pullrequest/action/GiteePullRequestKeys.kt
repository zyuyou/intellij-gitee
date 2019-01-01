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

package com.gitee.pullrequest.action

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeServerPath
import com.gitee.api.data.GiteeIssue
import com.gitee.api.data.GiteePullRequest
import com.gitee.api.data.GiteeRepoDetailed
import com.gitee.pullrequest.GiteePullRequestsComponentFactory
import com.gitee.pullrequest.data.GiteePullRequestDataProvider
import com.intellij.openapi.actionSystem.DataKey
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository

/**
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/pullrequest/action/GithubPullRequestKeys.kt
 * @author JetBrains s.r.o.
 */
object GiteePullRequestKeys {
  @JvmStatic
  val API_REQUEST_EXECUTOR =
    DataKey.create<GiteeApiRequestExecutor>("com.gitee.pullrequest.requestexecutor")

  @JvmStatic
  internal val PULL_REQUESTS_COMPONENT =
    DataKey.create<GiteePullRequestsComponentFactory.GiteePullRequestsComponent>("com.gitee.pullrequest.component")

  @JvmStatic
  val SELECTED_PULL_REQUEST = DataKey.create<GiteePullRequest>("com.gitee.pullrequest.selected")

  @JvmStatic
  val SELECTED_PULL_REQUEST_DATA_PROVIDER =
    DataKey.create<GiteePullRequestDataProvider>("com.gitee.pullrequest.selected.dataprovider")

  @JvmStatic
  val REPOSITORY = DataKey.create<GitRepository>("com.gitee.pullrequest.repository")

  @JvmStatic
  val REMOTE = DataKey.create<GitRemote>("com.gitee.pullrequest.remote")

  @JvmStatic
  val REPO_DETAILS = DataKey.create<GiteeRepoDetailed>("com.gitee.pullrequest.remote.repo.details")

  @JvmStatic
  val SERVER_PATH = DataKey.create<GiteeServerPath>("com.gitee.pullrequest.server.path")
}