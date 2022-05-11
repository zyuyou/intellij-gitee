// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.toolwindow

import com.gitee.pullrequest.data.GEPRIdentifier
import com.intellij.openapi.util.Key

interface GEPRToolWindowTabComponentController {

  val currentView: GEPRToolWindowViewType

  fun createPullRequest(requestFocus: Boolean = true)

  fun resetNewPullRequestView()

  fun viewList(requestFocus: Boolean = true)

  fun refreshList()

  fun viewPullRequest(id: GEPRIdentifier, requestFocus: Boolean = true, onShown: ((GEPRViewComponentController?) -> Unit)? = null)

  fun openPullRequestTimeline(id: GEPRIdentifier, requestFocus: Boolean)

  fun openPullRequestDiff(id: GEPRIdentifier, requestFocus: Boolean)

  fun openNewPullRequestDiff(requestFocus: Boolean)

  companion object {
    val KEY = Key.create<GEPRToolWindowTabComponentController>("Gitee.PullRequests.Toolwindow.Controller")
  }
}