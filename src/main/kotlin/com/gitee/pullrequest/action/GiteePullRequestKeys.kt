// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.action

import com.gitee.api.data.pullrequest.GEPullRequestShort
import com.intellij.openapi.actionSystem.DataKey

object GiteePullRequestKeys {
  @JvmStatic
  val ACTION_DATA_CONTEXT = DataKey.create<GiteePRActionDataContext>("org.jetbrains.plugins.gitee.pullrequest.datacontext")

  @JvmStatic
  internal val SELECTED_PULL_REQUEST = DataKey.create<GEPullRequestShort>("org.jetbrains.plugins.gitee.pullrequest.list.selected")
}