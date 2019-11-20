// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui

import java.util.*

interface GiteeLoadingModel<T> {
  val loading: Boolean

  val result: T?
  val error: Throwable?

  fun addStateChangeListener(listener: StateChangeListener)
  fun removeStateChangeListener(listener: StateChangeListener)

  interface StateChangeListener : EventListener {
    fun onLoadingStarted() {}
    fun onLoadingCompleted() {}
    fun onReset() {}
  }
}