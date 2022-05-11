// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.ui.component

import com.intellij.collaboration.ui.SimpleEventListener
import com.intellij.util.EventDispatcher
import javax.swing.Action
import kotlin.properties.Delegates

class GESimpleErrorPanelModel(override val errorPrefix: String) : GEErrorPanelModel {

  private val changeEventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)

  override var error by Delegates.observable<Throwable?>(null) { _, _, _ ->
    changeEventDispatcher.multicaster.eventOccurred()
  }
  override val errorAction: Action? = null


  override fun addAndInvokeChangeEventListener(listener: () -> Unit) =
    SimpleEventListener.addAndInvokeListener(changeEventDispatcher, listener)
}
