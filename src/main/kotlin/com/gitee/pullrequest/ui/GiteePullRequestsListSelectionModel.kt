/*
 *  Copyright 2016-2019 码云 - Gitee
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.gitee.pullrequest.ui

import com.gitee.api.data.GiteePullRequest
import com.intellij.openapi.Disposable
import com.intellij.util.EventDispatcher
import java.util.*
import kotlin.properties.Delegates

/**
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/pullrequest/ui/GithubPullRequestsListSelectionModel.kt
 * @author JetBrains s.r.o.
 */
internal class GiteePullRequestsListSelectionModel {
  var current: GiteePullRequest? by Delegates.observable<GiteePullRequest?>(null) { _, _, _ ->
    changeEventDispatcher.multicaster.selectionChanged()
  }

  private val changeEventDispatcher = EventDispatcher.create(SelectionChangedListener::class.java)

  fun addChangesListener(listener: SelectionChangedListener, disposable: Disposable) =
    changeEventDispatcher.addListener(listener, disposable)

  interface SelectionChangedListener : EventListener {
    fun selectionChanged()
  }
}