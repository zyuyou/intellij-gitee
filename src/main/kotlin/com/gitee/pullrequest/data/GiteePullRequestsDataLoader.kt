// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data

import com.intellij.openapi.Disposable
import org.jetbrains.annotations.CalledInAwt

internal interface GiteePullRequestsDataLoader : Disposable {
  @CalledInAwt
  fun getDataProvider(number: Long): GiteePullRequestDataProvider

  @CalledInAwt
  fun findDataProvider(number: Long): GiteePullRequestDataProvider?

  @CalledInAwt
  fun invalidateAllData()

  @CalledInAwt
  fun addInvalidationListener(disposable: Disposable, listener: (Long) -> Unit)
}