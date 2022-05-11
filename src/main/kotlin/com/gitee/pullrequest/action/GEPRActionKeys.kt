// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.action

import com.gitee.api.data.pullrequest.GEPullRequestShort
import com.gitee.pullrequest.data.provider.GEPRDataProvider
import com.gitee.pullrequest.ui.toolwindow.GEPRToolWindowTabComponentController
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.vcs.FilePath
import git4idea.repo.GitRepository

object GEPRActionKeys {
  @JvmStatic
  val GIT_REPOSITORY = DataKey.create<GitRepository>("com.gitee.pullrequest.git.repository")

  @JvmStatic
  val PULL_REQUEST_DATA_PROVIDER = DataKey.create<GEPRDataProvider>("com.gitee.pullrequest.data.provider")

  internal val PULL_REQUEST_FILES = DataKey.create<Iterable<FilePath>>("com.gitee.pullrequest.files")

  @JvmStatic
  val SELECTED_PULL_REQUEST = DataKey.create<GEPullRequestShort>("com.gitee.pullrequest.list.selected")

  @JvmStatic
  val PULL_REQUESTS_TAB_CONTROLLER = DataKey.create<GEPRToolWindowTabComponentController>("com.gitee.pullrequest.tab.controller")
}