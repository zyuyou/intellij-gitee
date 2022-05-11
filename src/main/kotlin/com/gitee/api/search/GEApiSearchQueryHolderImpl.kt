// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.search

import com.gitee.pullrequest.data.GEPRSearchQuery
import com.gitee.util.GiteeUtil.Delegates.observableField
import com.intellij.collaboration.ui.SimpleEventListener
import com.intellij.openapi.Disposable
import com.intellij.util.EventDispatcher

internal class GEApiSearchQueryHolderImpl : GEApiSearchQueryHolder {

  private val queryChangeEventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)

  override var queryString by observableField("", queryChangeEventDispatcher)

  override var query: GEPRSearchQuery
    get() = GEPRSearchQuery.parseFromString(queryString)
    set(value) {
      queryString = value.toString()
    }

  override fun addQueryChangeListener(disposable: Disposable, listener: () -> Unit) =
    SimpleEventListener.addDisposableListener(queryChangeEventDispatcher, disposable, listener)
}