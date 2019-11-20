// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest

import com.gitee.icons.GiteeIcons
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.Icon

internal class GiteePRFileType : FileType {
  override fun getName() = "GithubPullRequest"
  override fun getDescription() = "Github Pull Request"
  override fun getDefaultExtension() = ""
  override fun getIcon(): Icon? = GiteeIcons.PullRequestOpen
  override fun isBinary() = true
  override fun isReadOnly() = true
  override fun getCharset(file: VirtualFile, content: ByteArray): String? = null

  companion object {
    @JvmStatic
    val INSTANCE: FileType = GiteePRFileType()
  }
}