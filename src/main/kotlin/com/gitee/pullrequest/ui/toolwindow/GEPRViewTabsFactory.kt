// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.toolwindow

import com.gitee.i18n.GiteeBundle
import com.gitee.i18n.GiteeBundle.messagePointer
import com.intellij.collaboration.ui.codereview.CodeReviewTabs.bindTabText
import com.intellij.collaboration.ui.codereview.CodeReviewTabs.bindTabUi
import com.intellij.collaboration.ui.codereview.ReturnToListComponent
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.AppUIExecutor.onUiThread
import com.intellij.openapi.application.impl.coroutineDispatchingContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.tabs.JBTabs
import com.intellij.ui.tabs.TabInfo
import com.intellij.ui.tabs.TabsListener
import com.intellij.ui.tabs.impl.SingleHeightTabs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import javax.swing.JComponent

internal class GEPRViewTabsFactory(private val project: Project,
                                   private val backToListAction: () -> Unit,
                                   private val disposable: Disposable) {

  private val uiDisposable = Disposer.newDisposable().also {
    Disposer.register(disposable, it)
  }
  private val scope = CoroutineScope(SupervisorJob() + onUiThread().coroutineDispatchingContext())
    .also { Disposer.register(uiDisposable) { it.cancel() } }

  fun create(infoComponent: JComponent,
             diffController: GEPRDiffController,
             filesComponent: JComponent,
             filesCountModel: Flow<Int?>,
             notViewedFilesCountModel: Flow<Int?>?,
             commitsComponent: JComponent,
             commitsCountModel: Flow<Int?>): JBTabs {
    return create(infoComponent, filesComponent, filesCountModel, notViewedFilesCountModel, commitsComponent, commitsCountModel).also {
      val listener = object : TabsListener {
        override fun selectionChanged(oldSelection: TabInfo?, newSelection: TabInfo?) {
          diffController.activeTree = when (newSelection?.component) {
            filesComponent -> GEPRDiffController.ActiveTree.FILES
            commitsComponent -> GEPRDiffController.ActiveTree.COMMITS
            else -> null
          }
        }
      }
      it.addListener(listener)
      listener.selectionChanged(null, it.selectedInfo)
    }
  }

  private fun create(infoComponent: JComponent,
                     filesComponent: JComponent,
                     filesCountModel: Flow<Int?>,
                     notViewedFilesCountModel: Flow<Int?>?,
                     commitsComponent: JComponent,
                     commitsCountModel: Flow<Int?>): JBTabs {

    val infoTabInfo = TabInfo(infoComponent).apply {
      text = GiteeBundle.message("pull.request.info")
      sideComponent = createReturnToListSideComponent()
    }
    val filesTabInfo = TabInfo(filesComponent).apply {
      sideComponent = createReturnToListSideComponent()
    }
    val commitsTabInfo = TabInfo(commitsComponent).apply {
      sideComponent = createReturnToListSideComponent()
    }.also {
      scope.bindTabText(it, messagePointer("pull.request.commits"), commitsCountModel)
    }

    val tabs = object : SingleHeightTabs(project, uiDisposable) {
      override fun adjust(each: TabInfo?) = Unit
    }.apply {
      addTab(infoTabInfo)
      addTab(filesTabInfo)
      addTab(commitsTabInfo)
    }

    // after adding to `JBTabs` as `getTabLabel()` is used in `bindTabUi`
    if (notViewedFilesCountModel == null) {
      scope.bindTabText(filesTabInfo, messagePointer("pull.request.files"), filesCountModel)
    }
    else {
      scope.bindTabUi(tabs, filesTabInfo, messagePointer("pull.request.files"), filesCountModel, notViewedFilesCountModel)
    }

    return tabs
  }

  private fun createReturnToListSideComponent(): JComponent {
    return ReturnToListComponent.createReturnToListSideComponent(GiteeBundle.message("pull.request.back.to.list")) {
      backToListAction()
    }
  }
}