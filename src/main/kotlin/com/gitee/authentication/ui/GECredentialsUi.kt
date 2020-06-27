// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.ui

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeServerPath
import com.gitee.ui.util.Validator
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.layout.LayoutBuilder
import com.intellij.ui.layout.panel
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.UIUtil.getRegularPanelInsets
import javax.swing.JComponent
import javax.swing.JPanel

internal abstract class GECredentialsUi {
  abstract fun getPreferredFocusableComponent(): JComponent?
  abstract fun getValidator(): Validator
  abstract fun createExecutor(): GiteeApiRequestExecutor
  abstract fun acquireLoginAndToken(
    server: GiteeServerPath,
    executor: GiteeApiRequestExecutor,
    indicator: ProgressIndicator
  ): Triple<String, String, String>

  abstract fun handleAcquireError(error: Throwable): ValidationInfo
  abstract fun setBusy(busy: Boolean)

  var footer: LayoutBuilder.() -> Unit = { }

  fun getPanel(): JPanel =
    panel {
      centerPanel()
      footer()
    }.apply {
      // Border is required to have more space - otherwise there could be issues with focus ring.
      // `getRegularPanelInsets()` is used to simplify border calculation for dialogs where this panel is used.
      border = JBEmptyBorder(getRegularPanelInsets())
    }

  protected abstract fun LayoutBuilder.centerPanel()
}