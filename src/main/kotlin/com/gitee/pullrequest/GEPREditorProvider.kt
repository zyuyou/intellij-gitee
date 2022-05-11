// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest

import com.gitee.pullrequest.data.GEPRDataContextRepository
import com.intellij.diff.editor.DiffRequestProcessorEditorCustomizer
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.impl.EditorWindow
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile

internal class GEPREditorProvider : FileEditorProvider, DumbAware {

  override fun accept(project: Project, file: VirtualFile): Boolean {
    return file is GERepoVirtualFile && GEPRDataContextRepository.getInstance(project).findContext(file.repository) != null
  }

  override fun createEditor(project: Project, file: VirtualFile): FileEditor {
    file as GERepoVirtualFile
    val dataContext = GEPRDataContextRepository.getInstance(project).findContext(file.repository)!!


    return when (file) {
      is GEPRVirtualFile -> {
        val dataDisposable = Disposer.newDisposable()
        val dataProvider = dataContext.dataProviderRepository.getDataProvider(file.pullRequest, dataDisposable)
        when (file) {
          is GEPRDiffVirtualFile -> {
            GEPRDiffFileEditor(project, dataProvider.diffRequestModel, file).also { editor ->
              editor.putUserData(EditorWindow.HIDE_TABS, true)
              DiffRequestProcessorEditorCustomizer.customize(file, editor, editor.diffProcessor)
            }
          }
          is GEPRTimelineVirtualFile -> GEPRTimelineFileEditor(project, dataContext, dataProvider, file)
          else -> error("Unsupported file type")
        }.also {
          Disposer.register(it, dataDisposable)
        }
      }
      is GENewPRDiffVirtualFile -> GEPRDiffFileEditor(project, dataContext.newPRDiffModel, file).also { editor ->
        editor.putUserData(EditorWindow.HIDE_TABS, true)
        DiffRequestProcessorEditorCustomizer.customize(file, editor, editor.diffProcessor)
      }
      else -> error("Unsupported file type")
    }
  }

  override fun getEditorTypeId(): String = "GEPR"
  override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}