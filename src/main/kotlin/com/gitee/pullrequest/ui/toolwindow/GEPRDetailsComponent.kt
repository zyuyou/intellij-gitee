// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.toolwindow

import com.gitee.i18n.GiteeBundle
import com.gitee.pullrequest.action.GEPRReloadStateAction
import com.gitee.pullrequest.comment.convertToHtml
import com.gitee.pullrequest.data.service.GEPRSecurityService
import com.gitee.pullrequest.ui.details.*
import com.gitee.pullrequest.ui.timeline.GEPRTitleComponent
import com.gitee.ui.avatars.GEAvatarIconsProvider
import com.gitee.ui.util.HtmlEditorPane
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.PopupHandler
import com.intellij.ui.SideBorder
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UI
import com.intellij.util.ui.UIUtil
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.JPanel

internal object GEPRDetailsComponent {

  fun create(project: Project,
             securityService: GEPRSecurityService,
             avatarIconsProvider: GEAvatarIconsProvider,
             branchesModel: GEPRBranchesModel,
             detailsModel: GEPRDetailsModel,
             metadataModel: GEPRMetadataModel,
             stateModel: GEPRStateModel
  ): JComponent {
    val actionManager = ActionManager.getInstance()

    val branches = GEPRBranchesPanel.create(branchesModel)
    val title = GEPRTitleComponent.create(detailsModel)
    val description = HtmlEditorPane().apply {
      detailsModel.addAndInvokeDetailsChangedListener {
        setBody(detailsModel.description.convertToHtml(project))
      }
    }
    val timelineLink = ActionLink(GiteeBundle.message("pull.request.view.conversations.action")) {
      val action = ActionManager.getInstance().getAction("Gitee.PullRequest.Timeline.Show") ?: return@ActionLink
      ActionUtil.invokeAction(action, it.source as ActionLink, ActionPlaces.UNKNOWN, null, null)
    }
    val metadata = GEPRMetadataPanelFactory(metadataModel, avatarIconsProvider).create()
    val state = GEPRStatePanel(securityService, stateModel).also {
      detailsModel.addAndInvokeDetailsChangedListener {
        it.select(detailsModel.state, true)
      }
      PopupHandler.installPopupMenu(it, DefaultActionGroup(GEPRReloadStateAction()), "GEPRStatePanelPopup")
    }

    metadata.border = BorderFactory.createCompoundBorder(IdeBorderFactory.createBorder(SideBorder.TOP),
                                                         JBUI.Borders.empty(8))

    state.border = BorderFactory.createCompoundBorder(IdeBorderFactory.createBorder(SideBorder.TOP),
                                                      JBUI.Borders.empty(8))

    val detailsSection = JPanel(MigLayout(LC().insets("0", "0", "0", "0")
                                            .gridGap("0", "0")
                                            .fill().flowY())).apply {
      isOpaque = false
      border = JBUI.Borders.empty(8)

      add(branches, CC().gapBottom("${UI.scale(8)}"))
      add(title, CC().gapBottom("${UI.scale(8)}"))
      add(description, CC().grow().push().minHeight("0"))
      add(timelineLink, CC().gapBottom("push"))
    }

    val groupId = "Gitee.PullRequest.Details.Popup"
    PopupHandler.installPopupMenu(detailsSection, groupId, groupId)
    PopupHandler.installPopupMenu(description, groupId, groupId)
    PopupHandler.installPopupMenu(metadata, groupId, groupId)

    return JPanel(MigLayout(LC().insets("0", "0", "0", "0")
                              .gridGap("0", "0")
                              .fill().flowY())).apply {
      isOpaque = false

      add(detailsSection, CC().grow().push().minHeight("0"))
      add(metadata, CC().growX().pushX())
      add(Wrapper(state).apply {
        isOpaque = true
        background = UIUtil.getPanelBackground()
      }, CC().growX().pushX().minHeight("pref"))
    }
  }
}