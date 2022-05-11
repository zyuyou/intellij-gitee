// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest

import com.intellij.icons.AllIcons
import com.intellij.ide.FileIconProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

class GEPRVirtualFileIconProvider : FileIconProvider {

  override fun getIcon(file: VirtualFile, flags: Int, project: Project?): Icon? {
    return when (file) {
      is GENewPRDiffVirtualFile -> AllIcons.Actions.Diff
      is GEPRTimelineVirtualFile -> file.getIcon()
      is GEPRDiffVirtualFile -> AllIcons.Actions.Diff
      else -> null
    }
  }
}