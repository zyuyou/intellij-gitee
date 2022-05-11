// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.toolwindow

import com.intellij.openapi.util.Key

interface GEPRToolWindowTabController {

  /**
   * Initial view that will be displayed after login and data pre-loading
   * Only viable options are [GEPRToolWindowViewType.LIST] and [GEPRToolWindowViewType.NEW]
   */
  var initialView: GEPRToolWindowViewType

  val componentController: GEPRToolWindowTabComponentController?

  fun canResetRemoteOrAccount(): Boolean
  fun resetRemoteAndAccount()

  companion object {
    val KEY = Key.create<GEPRToolWindowTabController>("Gitee.PullRequests.ToolWindow.Tab.Controller")
  }
}