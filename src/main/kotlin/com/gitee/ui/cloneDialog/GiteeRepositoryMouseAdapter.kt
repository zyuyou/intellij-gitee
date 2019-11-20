// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.ui.cloneDialog

import com.intellij.ui.ListUtil
import com.intellij.ui.SimpleColoredComponent
import com.intellij.util.ui.UIUtil
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JList

internal class GiteeRepositoryMouseAdapter(private val list: JList<*>) : MouseAdapter() {
  private fun getRunnableAt(e: MouseEvent): Runnable? {
    val point = e.point
    val renderer = ListUtil.getDeepestRendererChildComponentAt(list, point)
    if (renderer !is SimpleColoredComponent) return null
    val tag = renderer.getFragmentTagAt(point.x)
    return if (tag is Runnable) tag else null
  }

  override fun mouseMoved(e: MouseEvent) {
    val runnable = getRunnableAt(e)
    if (runnable != null) {
      UIUtil.setCursor(list, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
    }
    else {
      UIUtil.setCursor(list, Cursor.getDefaultCursor())
    }
  }

  override fun mouseClicked(e: MouseEvent) {
    getRunnableAt(e)?.run()
  }
}