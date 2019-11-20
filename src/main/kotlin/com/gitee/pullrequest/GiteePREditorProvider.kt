// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

internal class GiteePREditorProvider : FileEditorProvider, DumbAware {
  override fun accept(project: Project, file: VirtualFile) = file is GiteePRVirtualFile
  override fun createEditor(project: Project, file: VirtualFile) = GiteePRFileEditor(
    ProgressManager.getInstance(),
    FileTypeRegistry.getInstance(),
    project,
    EditorFactory.getInstance(),
    file as GiteePRVirtualFile)

  override fun getEditorTypeId(): String = "GiteePR"
  override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}