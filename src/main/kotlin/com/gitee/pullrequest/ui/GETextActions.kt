// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui

import com.gitee.i18n.GiteeBundle
import com.intellij.CommonBundle
import com.intellij.collaboration.ui.codereview.InlineIconButton
import com.intellij.icons.AllIcons
import com.intellij.openapi.ui.MessageDialogBuilder
import icons.CollaborationToolsIcons
import java.awt.event.ActionListener
import java.util.concurrent.CompletableFuture
import javax.swing.JComponent

internal object GETextActions {
  fun createDeleteButton(delete: () -> CompletableFuture<out Any?>): JComponent {
    val icon = CollaborationToolsIcons.Delete
    val hoverIcon = CollaborationToolsIcons.DeleteHovered
    val button = InlineIconButton(icon, hoverIcon, tooltip = CommonBundle.message("button.delete"))
    button.actionListener = ActionListener {
      if (MessageDialogBuilder.yesNo(GiteeBundle.message("pull.request.review.comment.delete.dialog.title"),
                                     GiteeBundle.message("pull.request.review.comment.delete.dialog.msg")).ask(button)) {
        delete()
      }
    }
    return button
  }

  fun createEditButton(paneHandle: GEEditableHtmlPaneHandle): InlineIconButton {
    return createEditButton().apply {
      actionListener = ActionListener {
        paneHandle.showAndFocusEditor()
      }
    }
  }

  fun createEditButton(): InlineIconButton {
    val icon = AllIcons.General.Inline_edit
    val hoverIcon = AllIcons.General.Inline_edit_hovered
    return InlineIconButton(icon, hoverIcon, tooltip = CommonBundle.message("button.edit"))
  }
}