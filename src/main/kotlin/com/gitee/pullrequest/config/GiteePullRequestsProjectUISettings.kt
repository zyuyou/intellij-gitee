// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.config

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.util.EventDispatcher
import java.util.*

@State(name = "GiteePullRequestsUISettings", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
class GiteePullRequestsProjectUISettings : PersistentStateComponentWithModificationTracker<GiteePullRequestsProjectUISettings.SettingsState> {
  private var state: SettingsState = SettingsState()

  class SettingsState : BaseState() {
    var hiddenUrls by stringSet()
    var zipChanges by property(false)
  }

  private val changesEventDispatcher = EventDispatcher.create(ChangesEventDispatcher::class.java)

  fun addChangesListener(disposable: Disposable, listener: () -> Unit) {
    changesEventDispatcher.addListener(object : ChangesEventDispatcher {
      override fun stateChanged() {
        listener()
      }
    }, disposable)
  }

  fun getHiddenUrls(): Set<String> = state.hiddenUrls.toSet()

  fun addHiddenUrl(url: String) {
    if (state.hiddenUrls.add(url)) {
      state.intIncrementModificationCount()
    }
  }

  fun removeHiddenUrl(url: String) {
    if (state.hiddenUrls.remove(url)) {
      state.intIncrementModificationCount()
    }
  }

  var zipChanges: Boolean
    get() = state.zipChanges
    set(value) {
      state.zipChanges = value
      changesEventDispatcher.multicaster.stateChanged()
    }

  override fun getStateModificationCount() = state.modificationCount
  override fun getState() = state
  override fun loadState(state: GiteePullRequestsProjectUISettings.SettingsState) {
    this.state = state
  }

  companion object {
    @JvmStatic
    fun getInstance(project: Project) = project.service<GiteePullRequestsProjectUISettings>()

    private interface ChangesEventDispatcher : EventListener {
      fun stateChanged()
    }
  }
}
