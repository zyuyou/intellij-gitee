// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.details

import com.gitee.api.data.GiteeIssueLabel
import com.gitee.api.data.GiteeUser
import com.gitee.api.data.pullrequest.GEPullRequestRequestedReviewer
import com.gitee.i18n.GiteeBundle
import com.gitee.ui.avatars.GEAvatarIconsProvider
import com.gitee.ui.component.LabeledListPanelHandle
import com.gitee.ui.util.GEUIUtil
import com.gitee.util.CollectionDelta
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import java.util.concurrent.CompletableFuture
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

class GEPRMetadataPanelFactory(private val model: GEPRMetadataModel,
                               private val avatarIconsProvider: GEAvatarIconsProvider
) {

  private val panel = JPanel(null)

  fun create(): JComponent {
    val reviewersHandle = ReviewersListPanelHandle()
    val assigneesHandle = AssigneesListPanelHandle()
    val labelsHandle = LabelsListPanelHandle()

    return panel.apply {
      isOpaque = false
      layout = MigLayout(LC()
                           .fillX()
                           .gridGap("0", "0")
                           .insets("0", "0", "0", "0"))

      addListPanel(this, reviewersHandle)
      addListPanel(this, assigneesHandle)
      addListPanel(this, labelsHandle)
    }
  }

  private inner class ReviewersListPanelHandle
    : LabeledListPanelHandle<GEPullRequestRequestedReviewer>(model,
                                                             GiteeBundle.message("pull.request.no.reviewers"),
                                                             "${GiteeBundle.message("pull.request.reviewers")}:") {

    override fun getItems(): List<GEPullRequestRequestedReviewer> = model.reviewers

    override fun getItemComponent(item: GEPullRequestRequestedReviewer) = createUserLabel(item)

    override fun showEditPopup(parentComponent: JComponent): CompletableFuture<CollectionDelta<GEPullRequestRequestedReviewer>>? {
      return GEUIUtil
        .showChooserPopup(GiteeBundle.message("pull.request.reviewers"), parentComponent,
                          GEUIUtil.SelectionListCellRenderer.PRReviewers(avatarIconsProvider),
                          model.reviewers, model.loadPotentialReviewers())
    }

    override fun adjust(indicator: ProgressIndicator, delta: CollectionDelta<GEPullRequestRequestedReviewer>) =
      model.adjustReviewers(indicator, delta)
  }


  private inner class AssigneesListPanelHandle
    : LabeledListPanelHandle<GiteeUser>(model,
                                     GiteeBundle.message("pull.request.unassigned"),
                                     "${GiteeBundle.message("pull.request.assignees")}:") {

    override fun getItems(): List<GiteeUser> = model.assignees

    override fun getItemComponent(item: GiteeUser) = createUserLabel(item)

    override fun showEditPopup(parentComponent: JComponent): CompletableFuture<CollectionDelta<GiteeUser>>? = GEUIUtil
      .showChooserPopup(GiteeBundle.message("pull.request.assignees"), parentComponent,
                        GEUIUtil.SelectionListCellRenderer.Users(avatarIconsProvider),
                        model.assignees, model.loadPotentialAssignees())

    override fun adjust(indicator: ProgressIndicator, delta: CollectionDelta<GiteeUser>) =
      model.adjustAssignees(indicator, delta)
  }

  private fun createUserLabel(user: GEPullRequestRequestedReviewer) = JLabel(user.shortName,
    avatarIconsProvider.getIcon(user.avatarUrl),
    SwingConstants.LEFT).apply {
    border = JBUI.Borders.empty(UIUtil.DEFAULT_VGAP, UIUtil.DEFAULT_HGAP / 2, UIUtil.DEFAULT_VGAP, UIUtil.DEFAULT_HGAP / 2)
  }

  private inner class LabelsListPanelHandle
    : LabeledListPanelHandle<GiteeIssueLabel>(model,
                                      GiteeBundle.message("pull.request.no.labels"),
                                      "${GiteeBundle.message("pull.request.labels")}:") {

    override fun getItems(): List<GiteeIssueLabel> = model.labels

    override fun getItemComponent(item: GiteeIssueLabel) = createLabelLabel(item)

    override fun showEditPopup(parentComponent: JComponent): CompletableFuture<CollectionDelta<GiteeIssueLabel>>? =
      GEUIUtil.showChooserPopup(GiteeBundle.message("pull.request.labels"), parentComponent,
                                GEUIUtil.SelectionListCellRenderer.Labels(),
                                model.labels, model.loadAssignableLabels())

    override fun adjust(indicator: ProgressIndicator, delta: CollectionDelta<GiteeIssueLabel>) =
      model.adjustLabels(indicator, delta)
  }

  private fun createLabelLabel(label: GiteeIssueLabel) = Wrapper(GEUIUtil.createIssueLabelLabel(label)).apply {
    border = JBUI.Borders.empty(UIUtil.DEFAULT_VGAP + 1, UIUtil.DEFAULT_HGAP / 2, UIUtil.DEFAULT_VGAP + 2, UIUtil.DEFAULT_HGAP / 2)
  }

  companion object {
    private fun addListPanel(panel: JPanel, handle: LabeledListPanelHandle<*>) {
      panel.add(handle.label, CC().alignY("top").width(":${handle.preferredLabelWidth}:"))
      panel.add(handle.panel, CC().minWidth("0").growX().pushX().wrap())
    }
  }
}