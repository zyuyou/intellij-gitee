// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.details

import com.gitee.api.data.pullrequest.GEPullRequestState

interface GEPRDetailsModel {

  val number: String
  val title: String
  val description: String
  val state: GEPullRequestState
  val isDraft: Boolean

  fun addAndInvokeDetailsChangedListener(listener: () -> Unit)
}