// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.comment.ui

import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.util.Disposer

class GiteePREditorReviewThreadsController(threadMap: GiteePREditorReviewThreadsModel,
                                           private val componentFactory: GiteePREditorReviewThreadComponentFactory,
                                           componentInlaysManager: EditorComponentInlaysManager) {
  private val inlayByThread = mutableMapOf<GiteePRReviewThreadModel, Inlay<*>>()

  init {
    for ((line, threads) in threadMap.threadsByLine) {
      for (thread in threads) {
        val inlay = componentInlaysManager.insertAfter(line, componentFactory.createComponent(thread)) ?: break
        inlayByThread[thread] = inlay
      }
    }

    threadMap.addChangesListener(object : GiteePREditorReviewThreadsModel.ChangesListener {
      override fun threadsAdded(line: Int, threads: List<GiteePRReviewThreadModel>) {
        for (thread in threads) {
          val inlay = componentInlaysManager.insertAfter(line, componentFactory.createComponent(thread)) ?: break
          inlayByThread[thread] = inlay
        }
      }

      override fun threadsRemoved(line: Int, threads: List<GiteePRReviewThreadModel>) {
        for (thread in threads) {
          val inlay = inlayByThread.remove(thread) ?: continue
          Disposer.dispose(inlay)
        }
      }
    })
  }
}