// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.ui.avatars

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.icons.GiteeIcons
import com.gitee.ui.util.GEUIUtil
import com.gitee.util.CachingGEUserAvatarLoader
import com.intellij.collaboration.ui.codereview.avatar.CachingAvatarIconsProvider
import java.awt.Image
import javax.swing.Icon

class GEAvatarIconsProvider(private val avatarsLoader: CachingGEUserAvatarLoader,
                            private val requestExecutor: GiteeApiRequestExecutor)
  : CachingAvatarIconsProvider<String>(GiteeIcons.DefaultAvatar) {

  fun getIcon(key: String?): Icon = super.getIcon(key, GEUIUtil.AVATAR_SIZE)

  override fun loadImage(key: String): Image? = avatarsLoader.requestAvatar(requestExecutor, key).get()
}