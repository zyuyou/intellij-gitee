// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest

import com.gitee.api.GERepositoryCoordinates
import com.gitee.pullrequest.data.GEPRDataContextRepository
import com.gitee.pullrequest.data.GEPRIdentifier
import com.gitee.pullrequest.data.SimpleGEPRIdentifier
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.vcs.editor.ComplexPathVirtualFileSystem
import com.intellij.vcs.editor.GsonComplexPathSerializer

internal class GEPRVirtualFileSystem : ComplexPathVirtualFileSystem<GEPRVirtualFileSystem.GEPRFilePath>(
  GsonComplexPathSerializer(GEPRFilePath::class.java)
) {
  override fun getProtocol() = PROTOCOL

  override fun findOrCreateFile(project: Project, path: GEPRFilePath): VirtualFile? {
    val filesManager = GEPRDataContextRepository.getInstance(project).findContext(path.repository)?.filesManager ?: return null
    val pullRequest = path.prId
    return when {
      pullRequest == null -> filesManager.newPRDiffFile
      path.isDiff -> filesManager.findDiffFile(pullRequest)
      else -> filesManager.findTimelineFile(pullRequest)
    }
  }

  fun getPath(fileManagerId: String,
              project: Project,
              repository: GERepositoryCoordinates,
              id: GEPRIdentifier?,
              isDiff: Boolean = false): String =
    getPath(GEPRFilePath(fileManagerId, project.locationHash, repository, id?.let { SimpleGEPRIdentifier(it) }, isDiff))

  data class GEPRFilePath(override val sessionId: String,
                          override val projectHash: String,
                          val repository: GERepositoryCoordinates,
                          val prId: SimpleGEPRIdentifier?,
                          val isDiff: Boolean) : ComplexPath

  companion object {
    private const val PROTOCOL = "gepr"

    fun getInstance() = service<VirtualFileManager>().getFileSystem(PROTOCOL) as GEPRVirtualFileSystem
  }
}
