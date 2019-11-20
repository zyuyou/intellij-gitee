// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui

import com.gitee.pullrequest.data.GiteePRListLoader
import com.gitee.pullrequest.data.GiteePullRequestsDataLoader
import com.gitee.ui.GiteeListLoaderPanel
import com.gitee.ui.HtmlInfoPanel
import com.intellij.openapi.Disposable
import com.intellij.openapi.progress.util.ProgressWindow
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.ui.StatusText
import com.intellij.vcs.log.ui.frame.ProgressStripe
import javax.swing.JComponent
import javax.swing.JPanel

internal class GiteePRListLoaderPanel(listLoader: GiteePRListLoader,
                                   private val dataLoader: GiteePullRequestsDataLoader,
                                   contentComponent: JComponent,
                                   filterComponent: JComponent)
  : GiteeListLoaderPanel<GiteePRListLoader>(listLoader, contentComponent), Disposable {

  private lateinit var progressStripe: ProgressStripe

  override fun createCenterPanel(content: JComponent): JPanel {
    val stripe = ProgressStripe(content, this,
      ProgressWindow.DEFAULT_PROGRESS_DIALOG_POSTPONE_TIME_MILLIS)
    progressStripe = stripe
    return stripe
  }

  override fun setLoading(isLoading: Boolean) {
    if (isLoading) progressStripe.startLoading() else progressStripe.stopLoading()
  }

  init {
    listLoader.addOutdatedStateChangeListener(this) {
      updateInfoPanel()
    }

    addToTop(filterComponent)
    resetFilter()
  }

  override fun displayEmptyStatus(emptyText: StatusText) {
    if (listLoader.filterNotEmpty) {
      emptyText.text = "No pull requests matching filters. "
      emptyText.appendSecondaryText("Reset Filters", SimpleTextAttributes.LINK_ATTRIBUTES) {
        resetFilter()
      }
    }
    else {
      emptyText.text = "No pull requests loaded. "
      emptyText.appendSecondaryText("Refresh", SimpleTextAttributes.LINK_ATTRIBUTES) {
        listLoader.reset()
      }
    }
  }

  private fun resetFilter() {
    listLoader.resetFilter()
  }

  override fun updateInfoPanel() {
    super.updateInfoPanel()
    if (infoPanel.isEmpty && listLoader.outdated) {
      infoPanel.setInfo("<html><body>The list is outdated. <a href=''>Refresh</a></body></html>",
        HtmlInfoPanel.Severity.INFO) {
        listLoader.reset()
        dataLoader.invalidateAllData()
      }
    }
  }
}