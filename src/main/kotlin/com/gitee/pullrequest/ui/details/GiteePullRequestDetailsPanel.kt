// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.details

import com.gitee.api.data.GiteePullRequestDetailed
import com.gitee.pullrequest.avatars.CachingGiteeAvatarIconsProvider
import com.gitee.pullrequest.data.GiteePullRequestsBusyStateTracker
import com.gitee.pullrequest.data.service.GiteePullRequestsMetadataService
import com.gitee.pullrequest.data.service.GiteePullRequestsSecurityService
import com.gitee.pullrequest.data.service.GiteePullRequestsStateService
import com.gitee.ui.util.SingleValueModel
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.openapi.util.Disposer
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SideBorder
import com.intellij.util.ui.ComponentWithEmptyText
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.StatusText
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import java.awt.Graphics
import java.awt.event.AdjustmentListener
import javax.swing.BorderFactory
import javax.swing.JPanel


internal class GiteePullRequestDetailsPanel(project: Project,
                                            model: SingleValueModel<GiteePullRequestDetailed?>,
                                            securityService: GiteePullRequestsSecurityService,
                                            busyStateTracker: GiteePullRequestsBusyStateTracker,
                                            metadataService: GiteePullRequestsMetadataService,
                                            stateService: GiteePullRequestsStateService,
                                            avatarIconsProviderFactory: CachingGiteeAvatarIconsProvider.Factory)
  : JPanel(), ComponentWithEmptyText, Disposable {

  private val emptyText = object : StatusText(this) {
    override fun isStatusVisible() = model.value == null
  }

  private val metaPanel = GiteePullRequestMetadataPanel(project, model, securityService, busyStateTracker, metadataService, avatarIconsProviderFactory).apply {
    border = JBUI.Borders.empty(4, 8, 4, 8)
  }
  private val descriptionPanel = GiteePullRequestDescriptionPanel(model).apply {
    border = JBUI.Borders.empty(4, 8, 8, 8)
  }
  private val statePanel = GiteePullRequestStatePanel(model, securityService, busyStateTracker, stateService).apply {
    border = BorderFactory.createCompoundBorder(IdeBorderFactory.createBorder(SideBorder.TOP),
                                                JBUI.Borders.empty(8))
  }

  init {
    layout = MigLayout(LC().flowY().fill()
                         .gridGap("0", "0")
                         .insets("0", "0", "0", "0"))
    isOpaque = false

    val scrollPane = ScrollPaneFactory.createScrollPane(ScrollablePanel(VerticalFlowLayout(0, 0)).apply {
      add(metaPanel)
      add(descriptionPanel)
      isOpaque = false
    }, true).apply {
      viewport.isOpaque = false
      isOpaque = false
    }
    add(scrollPane, CC().minWidth("0").minHeight("0").growX().growY().growPrioY(0).shrinkPrioY(0))
    add(statePanel, CC().growX().growY().growPrioY(1).shrinkPrioY(1).pushY())

    val verticalScrollBar = scrollPane.verticalScrollBar
    verticalScrollBar.addAdjustmentListener(AdjustmentListener {

      if (verticalScrollBar.maximum - verticalScrollBar.visibleAmount >= 1) {
        statePanel.border = BorderFactory.createCompoundBorder(IdeBorderFactory.createBorder(SideBorder.TOP),
                                                               JBUI.Borders.empty(8))
      }
      else {
        statePanel.border = JBUI.Borders.empty(8)
      }

    })

    Disposer.register(this, metaPanel)
    Disposer.register(this, descriptionPanel)
    Disposer.register(this, statePanel)
  }

  override fun getEmptyText() = emptyText

  override fun paintComponent(g: Graphics?) {
    super.paintComponent(g)
    emptyText.paint(this, g)
  }

  override fun dispose() {}
}