// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.comment.ui

import com.gitee.util.GiteeUtil.Delegates.equalVetoingObservable
import com.intellij.collaboration.ui.SimpleEventListener
import com.intellij.util.EventDispatcher

class GEPRDiffReviewViewOptionsModel(showThreads: Boolean, filterResolvedThreads: Boolean) {

  private val changesEventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)

  var showThreads by equalVetoingObservable(showThreads) {
    changesEventDispatcher.multicaster.eventOccurred()
  }

  var filterResolvedThreads by equalVetoingObservable(filterResolvedThreads) {
    changesEventDispatcher.multicaster.eventOccurred()
  }

  fun addChangesListener(listener: () -> Unit) = SimpleEventListener.addListener(changesEventDispatcher, listener)
}
