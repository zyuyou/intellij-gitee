// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.ui.util

import com.gitee.util.GiteeUtil.Delegates.observableField
import com.intellij.collaboration.ui.SimpleEventListener
import com.intellij.util.EventDispatcher
import javax.swing.text.PlainDocument

class DisableableDocument : PlainDocument() {

  private val eventDispatcher = EventDispatcher.create(SimpleEventListener::class.java)

  var enabled by observableField(true, eventDispatcher)

  fun addAndInvokeEnabledStateListener(listener: () -> Unit) = SimpleEventListener.addAndInvokeListener(eventDispatcher, listener)
}