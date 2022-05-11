// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.ui.component

import javax.swing.Action

interface GEErrorPanelModel {
  val errorPrefix: String
  val error: Throwable?
  val errorAction: Action?

  fun addAndInvokeChangeEventListener(listener: () -> Unit)
}
