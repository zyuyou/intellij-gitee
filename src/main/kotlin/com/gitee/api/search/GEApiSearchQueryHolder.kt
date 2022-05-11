// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.search

import com.gitee.pullrequest.data.GEPRSearchQuery
import com.intellij.openapi.Disposable
import com.intellij.util.concurrency.annotations.RequiresEdt

internal interface GEApiSearchQueryHolder {

  @get:RequiresEdt
  @set:RequiresEdt
  var queryString: String

  @get:RequiresEdt
  @set:RequiresEdt
  var query: GEPRSearchQuery

  fun addQueryChangeListener(disposable: Disposable, listener: () -> Unit)
}