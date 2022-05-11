// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.details

import com.gitee.i18n.GiteeBundle
import com.gitee.pullrequest.data.GEPRMergeabilityState
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.components.BrowserLink
import com.intellij.util.ui.EmptyIcon
import org.jetbrains.annotations.Nls
import java.awt.FlowLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

object GEPRStatusChecksComponent {

  fun create(mergeability: GEPRMergeabilityState): JComponent {
    val panel = JPanel(FlowLayout(FlowLayout.LEADING, 0, 0))
    val checksState = mergeability.checksState
    if (checksState == GEPRMergeabilityState.ChecksState.NONE) {
      panel.isVisible = false
    }
    else {
      val label = JLabel().apply {
        icon = when (checksState) {
          GEPRMergeabilityState.ChecksState.BLOCKING_BEHIND,
          GEPRMergeabilityState.ChecksState.BLOCKING_FAILING -> AllIcons.RunConfigurations.TestError
          GEPRMergeabilityState.ChecksState.FAILING -> AllIcons.RunConfigurations.TestFailed
          GEPRMergeabilityState.ChecksState.PENDING -> AllIcons.RunConfigurations.TestNotRan
          GEPRMergeabilityState.ChecksState.SUCCESSFUL -> AllIcons.RunConfigurations.TestPassed
          else -> EmptyIcon.ICON_16
        }
        text = when (checksState) {
          GEPRMergeabilityState.ChecksState.BLOCKING_BEHIND -> GiteeBundle.message("pull.request.branch.out.of.sync")
          GEPRMergeabilityState.ChecksState.BLOCKING_FAILING,
          GEPRMergeabilityState.ChecksState.FAILING,
          GEPRMergeabilityState.ChecksState.PENDING,
          GEPRMergeabilityState.ChecksState.SUCCESSFUL -> getChecksResultsText(mergeability.failedChecks,
                                                                               mergeability.pendingChecks,
                                                                               mergeability.successfulChecks)
          else -> ""
        }
      }

      with(panel) {
        add(label)
        add(createLink(mergeability.htmlUrl))
      }
    }
    return panel
  }

  @Nls
  private fun getChecksResultsText(failedChecks: Int, pendingChecks: Int, successfulChecks: Int): String {
    val results = mutableListOf<String>()
    failedChecks.takeIf { it > 0 }?.let {
      GiteeBundle.message("pull.request.checks.failing", it)
    }?.also {
      results.add(it)
    }

    pendingChecks.takeIf { it > 0 }?.let {
      GiteeBundle.message("pull.request.checks.pending", it)
    }?.also {
      results.add(it)
    }

    successfulChecks.takeIf { it > 0 }?.let {
      GiteeBundle.message("pull.request.checks.successful", it)
    }?.also {
      results.add(it)
    }

    val checksCount = failedChecks + pendingChecks + successfulChecks
    return StringUtil.join(results, ", ") + " " + GiteeBundle.message("pull.request.checks", checksCount)
  }

  private fun createLink(url: String) =
    BrowserLink(GiteeBundle.message("open.in.browser.link"), url)
}