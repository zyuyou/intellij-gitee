// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data

import com.intellij.openapi.Disposable
import com.intellij.util.EventDispatcher
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.annotations.CalledInAny
import org.jetbrains.annotations.CalledInAwt
import java.util.*

class GiteePRBusyStateTrackerImpl : GiteePRBusyStateTracker {
  private val busySet = ContainerUtil.newConcurrentSet<Long>()
  private val busyChangeEventDispatcher = EventDispatcher.create(GiteePullRequestBusyStateListener::class.java)

  @CalledInAwt
  override fun acquire(pullRequest: Long): Boolean {
    val ok = busySet.add(pullRequest)
    if (ok) busyChangeEventDispatcher.multicaster.busyStateChanged(pullRequest)
    return ok
  }

  @CalledInAwt
  override fun release(pullRequest: Long) {
    val ok = busySet.remove(pullRequest)
    if (ok) busyChangeEventDispatcher.multicaster.busyStateChanged(pullRequest)
  }

  @CalledInAny
  override fun isBusy(pullRequest: Long) = busySet.contains(pullRequest)

  @CalledInAwt
  override fun addPullRequestBusyStateListener(disposable: Disposable, listener: (Long) -> Unit) =
    busyChangeEventDispatcher.addListener(object : GiteePullRequestBusyStateListener {
      override fun busyStateChanged(pullRequest: Long) {
        listener(pullRequest)
      }
    }, disposable)


  private interface GiteePullRequestBusyStateListener : EventListener {
    fun busyStateChanged(pullRequest: Long)
  }
}