// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.timeline

import com.gitee.pullrequest.comment.ui.GEPRReviewThreadModel
import com.intellij.openapi.application.ApplicationBundle
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.SingleComponentCenteringLayout
import com.intellij.util.ui.UIUtil
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener

object GEPRReviewThreadsPanel {

  fun create(model: GEPRReviewThreadsModel, threadComponentFactory: (GEPRReviewThreadModel) -> JComponent): JComponent {
    val panel = JPanel(VerticalLayout(12)).apply {
      isOpaque = false
    }

    val loadingPanel = JPanel(SingleComponentCenteringLayout()).apply {
      isOpaque = false
      add(JLabel(ApplicationBundle.message("label.loading.page.please.wait")).apply {
        foreground = UIUtil.getContextHelpForeground()
      })
    }

    Controller(model, panel, loadingPanel, threadComponentFactory)

    return panel
  }

  private class Controller(private val model: GEPRReviewThreadsModel,
                           private val panel: JPanel,
                           private val loadingPanel: JPanel,
                           private val threadComponentFactory: (GEPRReviewThreadModel) -> JComponent) {
    init {
      model.addListDataListener(object : ListDataListener {
        override fun intervalRemoved(e: ListDataEvent) {
          for (i in e.index1 downTo e.index0) {
            panel.remove(i)
          }
          updateVisibility()
          panel.revalidate()
          panel.repaint()
        }

        override fun intervalAdded(e: ListDataEvent) {
          for (i in e.index0..e.index1) {
            panel.add(threadComponentFactory(model.getElementAt(i)), i)
          }
          updateVisibility()
          panel.revalidate()
          panel.repaint()
        }

        override fun contentsChanged(e: ListDataEvent) {
          if (model.loaded) panel.remove(loadingPanel)
          updateVisibility()
          panel.validate()
          panel.repaint()
        }
      })

      if (!model.loaded) {
        panel.add(loadingPanel)
      }
      else for (i in 0 until model.size) {
        panel.add(threadComponentFactory(model.getElementAt(i)), i)
      }
      updateVisibility()
    }

    private fun updateVisibility() {
      panel.isVisible = panel.componentCount > 0
    }
  }
}