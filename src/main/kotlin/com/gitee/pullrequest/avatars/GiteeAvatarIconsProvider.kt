package com.gitee.pullrequest.avatars

import org.jetbrains.annotations.CalledInAwt
import javax.swing.Icon

interface GiteeAvatarIconsProvider {
    @CalledInAwt
    fun getIcon(avatarUrl: String?): Icon
}