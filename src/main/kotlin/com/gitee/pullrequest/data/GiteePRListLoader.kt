// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data

import com.gitee.api.data.pullrequest.GEPullRequestShort
import com.intellij.openapi.Disposable
import org.jetbrains.annotations.CalledInAwt
import java.util.concurrent.CompletableFuture

internal interface GiteePRListLoader : GiteeListLoader {
  @get:CalledInAwt
  val outdated: Boolean
  @get:CalledInAwt
  val filterNotEmpty: Boolean

  @CalledInAwt
  fun reloadData(request: CompletableFuture<out GEPullRequestShort>)

  @CalledInAwt
  fun resetFilter()

  @CalledInAwt
  fun addOutdatedStateChangeListener(disposable: Disposable, listener: () -> Unit)
}