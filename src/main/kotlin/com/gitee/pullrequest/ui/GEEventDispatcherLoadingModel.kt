// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui

import com.intellij.util.EventDispatcher

abstract class GEEventDispatcherLoadingModel : GELoadingModel {
  protected val eventDispatcher = EventDispatcher.create(GELoadingModel.StateChangeListener::class.java)

  final override fun addStateChangeListener(listener: GELoadingModel.StateChangeListener) = eventDispatcher.addListener(listener)

  fun removeStateChangeListener(listener: GELoadingModel.StateChangeListener) = eventDispatcher.removeListener(listener)
}