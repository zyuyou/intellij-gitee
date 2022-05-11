// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui

import com.gitee.pullrequest.comment.ui.GECommentTextFieldFactory
import com.gitee.pullrequest.comment.ui.GECommentTextFieldModel
import com.gitee.ui.util.GEUIUtil
import com.intellij.CommonBundle
import com.intellij.collaboration.async.CompletableFutureUtil.successOnEdt
import com.intellij.openapi.project.Project
import com.intellij.ui.components.panels.VerticalLayout
import java.util.concurrent.CompletableFuture
import javax.swing.JComponent
import javax.swing.JPanel

internal class GEEditableHtmlPaneHandle(private val project: Project,
                                        private val paneComponent: JComponent,
                                        private val getSourceText: () -> String,
                                        private val updateText: (String) -> CompletableFuture<out Any?>) {

  val panel = JPanel(VerticalLayout(8, VerticalLayout.FILL)).apply {
    isOpaque = false
    add(paneComponent)
  }

  private var editor: JComponent? = null

  fun showAndFocusEditor() {
    if (editor == null) {
      val model = GECommentTextFieldModel(project, getSourceText()) { newText ->
        updateText(newText).successOnEdt {
          hideEditor()
        }
      }

      editor = GECommentTextFieldFactory(model).create(CommonBundle.message("button.submit"), onCancel = {
        hideEditor()
      })
      panel.add(editor!!)
      panel.validate()
      panel.repaint()
    }

    editor?.let { GEUIUtil.focusPanel(it) }
  }

  private fun hideEditor() {
    editor?.let {
      panel.remove(it)
      panel.revalidate()
      panel.repaint()
    }
    editor = null
  }
}