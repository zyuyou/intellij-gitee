// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest

import com.gitee.i18n.GiteeBundle
import com.intellij.collaboration.ui.codereview.diff.MutableDiffRequestChainProcessor
import com.intellij.diff.util.FileEditorBase
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vcs.FilePath
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update

internal class GEPRDiffFileEditor(project: Project,
                                  private val diffRequestModel: GEPRDiffRequestModel,
                                  private val file: GERepoVirtualFile)
  : FileEditorBase() {

  internal val diffProcessor = object : MutableDiffRequestChainProcessor(project, null) {
    override fun selectFilePath(filePath: FilePath) {
      diffRequestModel.selectedFilePath = filePath
    }
  }

  private val diffChainUpdateQueue =
    MergingUpdateQueue("updateDiffChainQueue", 100, true, null, this).apply {
      setRestartTimerOnAdd(true)
    }

  override fun isValid() = !Disposer.isDisposed(diffProcessor)

  init {
    Disposer.register(diffProcessor, Disposable {
      firePropertyChange(FileEditor.PROP_VALID, true, false)
    })

    diffRequestModel.addAndInvokeRequestChainListener(diffChainUpdateQueue) {
      val chain = diffRequestModel.requestChain
      diffChainUpdateQueue.run(Update.create(diffRequestModel) {
        diffProcessor.chain = chain
      })
    }
  }

  override fun dispose() {
    Disposer.dispose(diffProcessor)
    super.dispose()
  }

  override fun getName(): String = GiteeBundle.message("pull.request.editor.diff")

  override fun getComponent() = diffProcessor.component
  override fun getPreferredFocusedComponent() = diffProcessor.preferredFocusedComponent

  override fun selectNotify() = diffProcessor.updateRequest()

  override fun getFile() = file
}
