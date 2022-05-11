// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.ui.component

import com.gitee.pullrequest.ui.GELoadingErrorHandler
import com.intellij.collaboration.ui.SimpleEventListener
import com.intellij.util.EventDispatcher
import javax.swing.Action
import kotlin.properties.Delegates.observable

class GEHandledErrorPanelModel(override val errorPrefix: String,
                               private val errorHandler: GELoadingErrorHandler) : GEErrorPanelModel {

  private val changeEventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)

  override var error by observable<Throwable?>(null) { _, _, newValue ->
    errorAction = newValue?.let { errorHandler.getActionForError(it) }
    changeEventDispatcher.multicaster.eventOccurred()
  }
  override var errorAction: Action? = null
    private set


  override fun addAndInvokeChangeEventListener(listener: () -> Unit) =
    SimpleEventListener.addAndInvokeListener(changeEventDispatcher, listener)
}
