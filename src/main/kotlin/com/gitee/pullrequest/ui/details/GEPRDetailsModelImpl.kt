// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.details

import com.gitee.api.data.GiteePullRequestDetailed
import com.gitee.api.data.pullrequest.GEPullRequestState
import com.intellij.collaboration.ui.SingleValueModel

class GEPRDetailsModelImpl(private val valueModel: SingleValueModel<GiteePullRequestDetailed>) : GEPRDetailsModel {

  override val number: String
    get() = valueModel.value.number.toString()
  override val title: String
    get() = valueModel.value.title
  override val description: String
    get() = valueModel.value.body
  override val state: GEPullRequestState
    get() = valueModel.value.state
  override val isDraft: Boolean
    get() = valueModel.value.isDraft

  override fun addAndInvokeDetailsChangedListener(listener: () -> Unit) =
    valueModel.addAndInvokeListener { listener() }
}