// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data

import com.gitee.api.data.GiteePullRequestDetailed
import com.gitee.pullrequest.data.provider.GEPRDataProvider
import com.intellij.openapi.Disposable
import com.intellij.util.concurrency.annotations.RequiresEdt

internal interface GEPRDataProviderRepository : Disposable {
  @RequiresEdt
  fun getDataProvider(id: GEPRIdentifier, disposable: Disposable): GEPRDataProvider

  @RequiresEdt
  fun findDataProvider(id: GEPRIdentifier): GEPRDataProvider?

  @RequiresEdt
  fun addDetailsLoadedListener(disposable: Disposable, listener: (GiteePullRequestDetailed) -> Unit)
}