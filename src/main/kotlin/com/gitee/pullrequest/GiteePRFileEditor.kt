// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest

import com.gitee.pullrequest.action.GiteePullRequestKeys
import com.gitee.pullrequest.avatars.GiteeAvatarIconsProvider
import com.gitee.pullrequest.data.GiteePRTimelineLoader
import com.gitee.pullrequest.data.GiteePullRequestDataProvider
import com.gitee.pullrequest.ui.timeline.*
import com.gitee.ui.GiteeListLoaderPanel
import com.gitee.ui.util.SingleValueModel
import com.gitee.util.GiteeUIUtil
import com.gitee.util.handleOnEdt
import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ui.componentsList.components.ScrollablePanel
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.AsyncProcessIcon
import com.intellij.util.ui.ComponentWithEmptyText
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import javax.swing.JComponent
import javax.swing.JPanel

internal class GiteePRFileEditor(progressManager: ProgressManager,
                              private val fileTypeRegistry: FileTypeRegistry,
                              private val project: Project,
                              private val editorFactory: EditorFactory,
                              private val file: GiteePRVirtualFile)
  : UserDataHolderBase(), FileEditor {

  private val propertyChangeSupport = PropertyChangeSupport(this)
  private val mainPanel = Wrapper()
  private val contentPanel: JPanel

  init {
    val context = file.context

    val detailsModel = SingleValueModel(file.pullRequest)
    val timelineModel = GiteePRTimelineMergingModel()
    Disposer.register(this, Disposable {
      timelineModel.removeAll()
    })

    val repository = context.repositoryCoordinates
    val loader = GiteePRTimelineLoader(progressManager, context.requestExecutor, repository.serverPath, repository.repositoryPath,
      file.pullRequest.number, timelineModel)
    Disposer.register(this, loader)

    val dataProvider = file.dataProvider
    fun handleReviewsThreads() {
      dataProvider.reviewThreadsRequest.handleOnEdt(this) { threads, _ ->
        if (threads != null) timelineModel.setReviewsThreads(threads)
      }
    }

    fun handleDetails() {
      dataProvider.detailsRequest.handleOnEdt(this@GiteePRFileEditor) { pr, _ ->
        if (pr != null) detailsModel.value = pr
      }
    }
    dataProvider.addRequestsChangesListener(this, object : GiteePullRequestDataProvider.RequestsChangedListener {
      override fun detailsRequestChanged() = handleDetails()
      override fun reviewThreadsRequestChanged() = handleReviewsThreads()
    })
    handleDetails()
    handleReviewsThreads()

    val avatarIconsProvider = context.avatarIconsProviderFactory.create(GiteeUIUtil.avatarSize, mainPanel)

    val header = GiteePRHeaderPanel(detailsModel, avatarIconsProvider)
    val timeline = GiteePRTimelineComponent(timelineModel, createItemComponentFactory(timelineModel, avatarIconsProvider))
    val loadingIcon = AsyncProcessIcon("Loading").apply {
      isVisible = false
    }

    contentPanel = object : ScrollablePanel(), ComponentWithEmptyText, Disposable {
      init {
        isOpaque = false
        border = JBUI.Borders.empty(UIUtil.LARGE_VGAP, UIUtil.DEFAULT_HGAP * 2)

        val maxWidth = (GiteeUIUtil.getFontEM(this) * 42).toInt()

        layout = MigLayout(LC().gridGap("0", "0")
          .insets("0", "0", "0", "0")
          .fillX()
          .flowY()).apply {
          columnConstraints = "[:$maxWidth:$maxWidth]push"
        }

        emptyText.clear()

        add(header)
        add(timeline)
        add(loadingIcon, CC().alignX("center"))
      }

      override fun getEmptyText() = timeline.emptyText

      override fun dispose() {}
    }

    val loaderPanel = object : GiteeListLoaderPanel<GiteePRTimelineLoader>(loader, contentPanel, true), DataProvider {
      override val loadingText = ""

      override fun createCenterPanel(content: JComponent) = Wrapper(content)

      override fun setLoading(isLoading: Boolean) {
        loadingIcon.isVisible = isLoading
      }

      override fun updateUI() {
        super.updateUI()
        background = EditorColorsManager.getInstance().globalScheme.defaultBackground
      }

      override fun getData(dataId: String): Any? {
        if (GiteePullRequestKeys.ACTION_DATA_CONTEXT.`is`(dataId)) return context
        return null
      }
    }
    Disposer.register(this, loaderPanel)
    Disposer.register(loaderPanel, contentPanel)
    Disposer.register(contentPanel, loadingIcon)

    mainPanel.setContent(loaderPanel)
  }

  private fun createItemComponentFactory(timelineModel: GiteePRTimelineMergingModel, avatarIconsProvider: GiteeAvatarIconsProvider)
    : GiteePRTimelineItemComponentFactory {

    val diffFactory = GiteePRReviewThreadDiffComponentFactory(fileTypeRegistry, project, editorFactory)
    val eventsFactory = GiteePRTimelineEventComponentFactoryImpl(avatarIconsProvider)
    return GiteePRTimelineItemComponentFactory(avatarIconsProvider, timelineModel, diffFactory, eventsFactory)
  }

  override fun getName() = file.name

  override fun getComponent(): JComponent = mainPanel
  override fun getPreferredFocusedComponent(): JComponent? = contentPanel

  override fun getFile() = file
  override fun isModified(): Boolean = false
  override fun isValid(): Boolean = true

  override fun selectNotify() {}
  override fun deselectNotify() {}

  override fun addPropertyChangeListener(listener: PropertyChangeListener) = propertyChangeSupport.addPropertyChangeListener(listener)
  override fun removePropertyChangeListener(listener: PropertyChangeListener) = propertyChangeSupport.removePropertyChangeListener(listener)

  override fun setState(state: FileEditorState) {}
  override fun getBackgroundHighlighter(): BackgroundEditorHighlighter? = null

  override fun getCurrentLocation(): FileEditorLocation? = null

  override fun dispose() {}
}
