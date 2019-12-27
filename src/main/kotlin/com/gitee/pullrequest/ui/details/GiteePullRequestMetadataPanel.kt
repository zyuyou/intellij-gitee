// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.details

import com.gitee.api.data.GiteeIssueLabel
import com.gitee.api.data.GiteePullRequestDetailed
import com.gitee.api.data.GiteeUser
import com.gitee.pullrequest.avatars.CachingGiteeAvatarIconsProvider
import com.gitee.pullrequest.data.GiteePullRequestsBusyStateTracker
import com.gitee.pullrequest.data.service.GiteePullRequestsMetadataService
import com.gitee.pullrequest.data.service.GiteePullRequestsSecurityService
import com.gitee.ui.util.SingleValueModel
import com.gitee.util.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import org.jetbrains.annotations.Nls
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

internal class GiteePullRequestMetadataPanel(private val project: Project,
                                             private val model: SingleValueModel<GiteePullRequestDetailed?>,
                                             private val securityService: GiteePullRequestsSecurityService,
                                             private val busyStateTracker: GiteePullRequestsBusyStateTracker,
                                             private val metadataService: GiteePullRequestsMetadataService,
                                             private val avatarIconsProviderFactory: CachingGiteeAvatarIconsProvider.Factory)
  : JPanel(), Disposable {

  private val avatarIconsProvider = avatarIconsProviderFactory.create(GiteeUIUtil.avatarSize, this)

  private val directionPanel = GiteePullRequestDirectionPanel()
  private val reviewersHandle = ReviewersListPanelHandle()
  private val assigneesHandle = AssigneesListPanelHandle()
  private val labelsHandle = LabelsListPanelHandle()

  init {
    isOpaque = false
    layout = MigLayout(LC()
                         .fillX()
                         .gridGap("0", "0")
                         .insets("0", "0", "0", "0"))

    add(directionPanel, CC()
      .minWidth("0")
      .spanX(2).growX()
      .wrap())
    addListPanel(reviewersHandle)
    addListPanel(assigneesHandle)
    addListPanel(labelsHandle)

    fun update() {
      directionPanel.direction = model.value?.let { it.headLabel to it.baseRefName }
    }

    model.addValueChangedListener(this) {
      update()
    }
    update()

    Disposer.register(this, reviewersHandle)
    Disposer.register(this, assigneesHandle)
    Disposer.register(this, labelsHandle)
  }

  private fun addListPanel(handle: LabeledListPanelHandle<*>) {
    add(handle.label, CC().alignY("top"))
    add(handle.panel, CC().minWidth("0").growX().pushX().wrap())
  }

  override fun dispose() {}

  // PR详情测试人员列表
  private inner class ReviewersListPanelHandle
    : LabeledListPanelHandle<GiteeUser>(model, securityService, busyStateTracker, "No Reviewers", "Reviewers:") {
    override fun extractItems(details: GiteePullRequestDetailed): List<GiteeUser> = details.testers

    override fun getItemComponent(item: GiteeUser) = createUserLabel(item)

    override fun editList() {
      val details = model.value ?: return
      GiteeUIUtil
        .showChooserPopup("Reviewers", editButton, { list ->
          val avatarIconsProvider = avatarIconsProviderFactory.create(GiteeUIUtil.avatarSize, list)
          GiteeUIUtil.SelectionListCellRenderer.Users(avatarIconsProvider)
        }, details.testers, metadataService.collaboratorsWithPushAccess)
        .handleOnEdt(getAdjustmentHandler(details.number, "reviewer") { indicator, delta ->
          metadataService.adjustReviewers(indicator, details.number, delta)
        })
    }
  }

  // PR详情审查人员列表
  private inner class AssigneesListPanelHandle
    : LabeledListPanelHandle<GiteeUser>(model, securityService, busyStateTracker, "Unassigned", "Assignees:") {

    override fun extractItems(details: GiteePullRequestDetailed): List<GiteeUser> = details.assignees

    override fun getItemComponent(item: GiteeUser) = createUserLabel(item)

    override fun editList() {
      val details = model.value ?: return
      GiteeUIUtil
        .showChooserPopup("Assignees", editButton, { list ->
          val avatarIconsProvider = avatarIconsProviderFactory.create(GiteeUIUtil.avatarSize, list)
          GiteeUIUtil.SelectionListCellRenderer.Users(avatarIconsProvider)
        }, details.assignees, metadataService.collaboratorsWithPushAccess)
        .handleOnEdt(getAdjustmentHandler(details.number, "assignee") { indicator, delta ->
          metadataService.adjustAssignees(indicator, details.number, delta)
        })
    }
  }

  private fun createUserLabel(user: GiteeUser) = JLabel(user.login,
                                                     avatarIconsProvider.getIcon(user.avatarUrl),
                                                     SwingConstants.LEFT).apply {
    border = JBUI.Borders.empty(UIUtil.DEFAULT_VGAP, UIUtil.DEFAULT_HGAP / 2, UIUtil.DEFAULT_VGAP, UIUtil.DEFAULT_HGAP / 2)
  }

  // PR详情标签列表
  private inner class LabelsListPanelHandle
    : LabeledListPanelHandle<GiteeIssueLabel>(model, securityService, busyStateTracker, "No Labels", "Labels:") {

    override fun extractItems(details: GiteePullRequestDetailed): List<GiteeIssueLabel>? = details.labels

    override fun getItemComponent(item: GiteeIssueLabel) = createLabelLabel(item)

    override fun editList() {
      val details = model.value ?: return
      GiteeUIUtil
        .showChooserPopup("Labels", editButton, { GiteeUIUtil.SelectionListCellRenderer.Labels() }, details.labels, metadataService.labels)
        .handleOnEdt(getAdjustmentHandler(details.number, "label") { indicator, delta ->
          metadataService.adjustLabels(indicator, details.number, delta)
        })
    }
  }

  private fun createLabelLabel(label: GiteeIssueLabel) = Wrapper(GiteeUIUtil.createIssueLabelLabel(label)).apply {
    border = JBUI.Borders.empty(UIUtil.DEFAULT_VGAP + 1, UIUtil.DEFAULT_HGAP / 2, UIUtil.DEFAULT_VGAP + 2, UIUtil.DEFAULT_HGAP / 2)
  }

  private fun <T> getAdjustmentHandler(pullRequest: Long, @Nls entityName: String,
                                       adjuster: (ProgressIndicator, CollectionDelta<T>) -> Unit)
    : (CollectionDelta<T>?, Throwable?) -> Unit {

    return handler@{ delta, error ->
      if (error != null) {
        if (!GiteeAsyncUtil.isCancellation(error))
          GiteeNotifications.showError(project, "Failed to adjust list of ${StringUtil.pluralize(entityName)}", error)
        return@handler
      }
      if (delta == null || delta.isEmpty) {
        return@handler
      }

      if (!busyStateTracker.acquire(pullRequest)) return@handler
      object : Task.Backgroundable(project, "Adjusting List of ${StringUtil.pluralize(entityName).capitalize()}...",
                                   true) {
        override fun run(indicator: ProgressIndicator) {
          adjuster(indicator, delta)
        }

        override fun onThrowable(error: Throwable) {
          GiteeNotifications.showError(project, "Failed to adjust list of ${StringUtil.pluralize(entityName)}", error)
        }

        override fun onFinished() {
          busyStateTracker.release(pullRequest)
        }
      }.queue()
    }
  }
}