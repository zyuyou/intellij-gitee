// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.search

import com.gitee.ui.util.SingleValueModel
import com.intellij.openapi.Disposable

internal class GiteePullRequestSearchQueryHolderImpl : GiteePullRequestSearchQueryHolder {
  private val delegate = SingleValueModel(GiteePullRequestSearchQuery.parseFromString("state:open"))

  override var query: GiteePullRequestSearchQuery
    get() = delegate.value
    set(value) {
      delegate.value = value
    }

  override fun addQueryChangeListener(disposable: Disposable, listener: () -> Unit) =
    delegate.addValueChangedListener(disposable, listener)
}