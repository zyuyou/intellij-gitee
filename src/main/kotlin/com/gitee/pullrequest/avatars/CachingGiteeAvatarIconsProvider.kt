/*
 *  Copyright 2016-2019 码云 - Gitee
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.gitee.pullrequest.avatars

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.data.GiteeUser
import com.gitee.icons.GiteeIcons
import com.gitee.util.CachingGiteeUserAvatarLoader
import com.gitee.util.GiteeImageResizer
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.util.LowMemoryWatcher
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JBValue
import org.jetbrains.annotations.CalledInAwt
import java.awt.Component
import java.awt.Graphics
import java.awt.Image
import java.util.concurrent.CompletableFuture
import javax.swing.Icon

/**
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/pullrequest/avatars/CachingGiteeAvatarIconsProvider.kt
 * @param component which will be repainted when icons are loaded
 */
internal class CachingGiteeAvatarIconsProvider(private val avatarsLoader: CachingGiteeUserAvatarLoader,
                                               private val imagesResizer: GiteeImageResizer,
                                               private val requestExecutor: GiteeApiRequestExecutor,
                                               private val iconSize: JBValue,
                                               private val component: Component) : Disposable {

  private val scaleContext = JBUI.ScaleContext.create(component)
  private var defaultIcon = createDefaultIcon(iconSize.get())
  private val icons = mutableMapOf<GiteeUser, Icon>()

  init {
    LowMemoryWatcher.register(Runnable { icons.clear() }, this)
  }

  private fun createDefaultIcon(size: Int): Icon {
    val standardDefaultAvatar = GiteeIcons.DefaultAvatar
    val scale = size.toFloat() / standardDefaultAvatar.iconWidth.toFloat()
    return IconUtil.scale(standardDefaultAvatar, null, scale)
  }

  @CalledInAwt
  fun getIcon(user: GiteeUser): Icon {
    val iconSize = iconSize.get()

    // so that icons are rescaled when any scale changes (be it font size or current DPI)
    if (scaleContext.update(JBUI.ScaleContext.create(component))) {
      defaultIcon = createDefaultIcon(iconSize)
      icons.clear()
    }

    val modality = ModalityState.stateForComponent(component)
    return icons.getOrPut(user) {
      val icon = DelegatingIcon(defaultIcon)
      avatarsLoader
        .requestAvatar(requestExecutor, user)
        .thenCompose<Image?> {
          if (it != null) imagesResizer.requestImageResize(it, iconSize, scaleContext)
          else CompletableFuture.completedFuture(null)
        }
        .thenAccept {
          if (it != null) runInEdt(modality) {
            icon.delegate = IconUtil.createImageIcon(it)
            component.repaint()
          }
        }

      icon
    }
  }

  private class DelegatingIcon(var delegate: Icon) : Icon {
    override fun getIconHeight() = delegate.iconHeight
    override fun getIconWidth() = delegate.iconWidth
    override fun paintIcon(c: Component?, g: Graphics?, x: Int, y: Int) = delegate.paintIcon(c, g, x, y)
  }

  override fun dispose() {}

  // helper to avoid passing all the services to clients
  class Factory(private val avatarsLoader: CachingGiteeUserAvatarLoader,
                private val imagesResizer: GiteeImageResizer,
                private val requestExecutor: GiteeApiRequestExecutor) {
    fun create(iconSize: JBValue, component: Component) = CachingGiteeAvatarIconsProvider(avatarsLoader, imagesResizer,
                                                                                           requestExecutor, iconSize, component)
  }
}
