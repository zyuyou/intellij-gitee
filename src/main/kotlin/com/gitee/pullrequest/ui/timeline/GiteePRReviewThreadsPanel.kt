// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.timeline

import com.gitee.pullrequest.avatars.GiteeAvatarIconsProvider
import com.gitee.pullrequest.comment.ui.GiteePRReviewThreadModel
import com.gitee.pullrequest.comment.ui.GiteePRReviewThreadPanel
import com.intellij.ui.components.panels.VerticalBox
import com.intellij.util.ui.JBUI
import javax.swing.JComponent
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener

class GiteePRReviewThreadsPanel(model: GiteePRReviewThreadsModel,
                             private val avatarIconsProvider: GiteeAvatarIconsProvider,
                             private val reviewDiffComponentFactory: GiteePRReviewThreadDiffComponentFactory)
  : VerticalBox() {

  init {
    model.addListDataListener(object : ListDataListener {
      override fun intervalRemoved(e: ListDataEvent) {
        for (i in e.index1 downTo e.index0) {
          remove(i)
        }
        revalidate()
        repaint()
      }

      override fun intervalAdded(e: ListDataEvent) {
        for (i in e.index0..e.index1) {
          add(createComponent(model.getElementAt(i)), i)
        }
        revalidate()
        repaint()
      }

      override fun contentsChanged(e: ListDataEvent) {
        validate()
        repaint()
      }
    })

    for (i in 0 until model.size) {
      add(createComponent(model.getElementAt(i)), i)
    }
  }

  private fun createComponent(thread: GiteePRReviewThreadModel): JComponent {
    return JBUI.Panels.simplePanel(reviewDiffComponentFactory.createComponent(thread.filePath, thread.diffHunk))
      .addToBottom(GiteePRReviewThreadPanel(avatarIconsProvider, thread))
      .andTransparent()
  }
}