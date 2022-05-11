// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data

import com.gitee.api.GERepositoryCoordinates
import com.gitee.api.data.GiteePullRequest
import com.gitee.pullrequest.GENewPRDiffVirtualFile
import com.gitee.pullrequest.GEPRDiffVirtualFile
import com.gitee.pullrequest.GEPRStatisticsCollector
import com.gitee.pullrequest.GEPRTimelineVirtualFile
import com.intellij.diff.editor.DiffEditorTabFilesManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx
import com.intellij.openapi.project.Project
import com.intellij.util.EventDispatcher
import com.intellij.util.containers.ContainerUtil
import java.util.*

internal class GEPRFilesManagerImpl(private val project: Project,
                                    private val repository: GERepositoryCoordinates
) : GEPRFilesManager {

  // current time should be enough to distinguish the manager between launches
  private val id = System.currentTimeMillis().toString()

  private val filesEventDispatcher = EventDispatcher.create(FileListener::class.java)

  private val files = ContainerUtil.createWeakValueMap<GEPRIdentifier, GEPRTimelineVirtualFile>()
  private val diffFiles = ContainerUtil.createWeakValueMap<GEPRIdentifier, GEPRDiffVirtualFile>()
  override val newPRDiffFile by lazy { GENewPRDiffVirtualFile(id, project, repository) }

  override fun createAndOpenTimelineFile(pullRequest: GEPRIdentifier, requestFocus: Boolean) {
    files.getOrPut(SimpleGEPRIdentifier(pullRequest)) {
      GEPRTimelineVirtualFile(id, project, repository, pullRequest)
    }.let {
      filesEventDispatcher.multicaster.onBeforeFileOpened(it)
      FileEditorManager.getInstance(project).openFile(it, requestFocus)
      GEPRStatisticsCollector.logTimelineOpened(project)
    }
  }

  override fun createAndOpenDiffFile(pullRequest: GEPRIdentifier, requestFocus: Boolean) {
    diffFiles.getOrPut(SimpleGEPRIdentifier(pullRequest)) {
      GEPRDiffVirtualFile(id, project, repository, pullRequest)
    }.let {
      DiffEditorTabFilesManager.getInstance(project).showDiffFile(it, requestFocus)
      GEPRStatisticsCollector.logDiffOpened(project)
    }
  }

  override fun openNewPRDiffFile(requestFocus: Boolean) {
    DiffEditorTabFilesManager.getInstance(project).showDiffFile(newPRDiffFile, requestFocus)
  }

  override fun findTimelineFile(pullRequest: GEPRIdentifier): GEPRTimelineVirtualFile? = files[SimpleGEPRIdentifier(pullRequest)]

  override fun findDiffFile(pullRequest: GEPRIdentifier): GEPRDiffVirtualFile? = diffFiles[SimpleGEPRIdentifier(pullRequest)]

  override fun updateTimelineFilePresentation(details: GiteePullRequest) {
    val file = findTimelineFile(details)
    if (file != null) {
      file.details = details
      FileEditorManagerEx.getInstanceEx(project).updateFilePresentation(file)
    }
  }

  override fun addBeforeTimelineFileOpenedListener(disposable: Disposable, listener: (file: GEPRTimelineVirtualFile) -> Unit) {
    filesEventDispatcher.addListener(object : FileListener {
      override fun onBeforeFileOpened(file: GEPRTimelineVirtualFile) = listener(file)
    }, disposable)
  }

  override fun dispose() {
    for (file in (files.values + diffFiles.values)) {
      FileEditorManager.getInstance(project).closeFile(file)
      file.isValid = false
    }
  }

  private interface FileListener : EventListener {
    fun onBeforeFileOpened(file: GEPRTimelineVirtualFile)
  }
}
