// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.changes

import com.gitee.pullrequest.ui.SimpleEventListener
import com.intellij.util.EventDispatcher
import git4idea.GitCommit
import kotlin.properties.Delegates.observable

class GiteePRChangesModelImpl(zipChanges: Boolean) : GiteePRChangesModel {
  override var zipChanges by observable(zipChanges) { _, _, _ ->
    eventDispatcher.multicaster.eventOccurred()
  }
  override var commits by observable<List<GitCommit>>(emptyList()) { _, _, _ ->
    eventDispatcher.multicaster.eventOccurred()
  }

  private val eventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)

  override fun addStateChangesListener(listener: () -> Unit) {
    eventDispatcher.addListener(object : SimpleEventListener {
      override fun eventOccurred() {
        listener()
      }
    })
  }
}