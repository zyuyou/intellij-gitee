// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.comment.ui

import com.gitee.pullrequest.avatars.CachingGiteeAvatarIconsProvider
import com.gitee.util.GiteeUIUtil
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.GraphicsUtil
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.geom.RoundRectangle2D
import javax.swing.JComponent

class GiteePREditorReviewThreadComponentFactoryImpl
internal constructor(private val avatarIconsProviderFactory: CachingGiteeAvatarIconsProvider.Factory)
  : GiteePREditorReviewThreadComponentFactory {

  override fun createComponent(thread: GiteePRReviewThreadModel): JComponent {
    val wrapper = RoundedPanel().apply {
      border = IdeBorderFactory.createRoundedBorder(10, 1)
    }
    val avatarIconsProvider = avatarIconsProviderFactory.create(GiteeUIUtil.avatarSize, wrapper)

    val panel = GiteePRReviewThreadPanel(avatarIconsProvider, thread).apply {
      border = JBUI.Borders.empty(0, UIUtil.DEFAULT_HGAP)
    }
    wrapper.setContent(panel)
    return wrapper
  }

  private class RoundedPanel : Wrapper() {
    override fun paintComponent(g: Graphics) {
      GraphicsUtil.setupRoundedBorderAntialiasing(g)

      val g2 = g as Graphics2D
      val rect = Rectangle(size)
      JBInsets.removeFrom(rect, insets)
      g2.color = background
      g2.fill(RoundRectangle2D.Float(rect.x.toFloat(), rect.y.toFloat(), rect.width.toFloat(), rect.height.toFloat(), 10f, 10f))
    }
  }
}