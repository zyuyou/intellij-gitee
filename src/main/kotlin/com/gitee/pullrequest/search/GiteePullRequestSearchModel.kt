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

package com.gitee.pullrequest.search

import com.intellij.openapi.Disposable
import com.intellij.util.EventDispatcher
import org.jetbrains.annotations.CalledInAwt
import java.util.*
import kotlin.properties.Delegates

/**
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/pullrequest/search/GithubPullRequestSearchModel.kt
 * @author JetBrains s.r.o.
 */
internal class GiteePullRequestSearchModel {
  @get:CalledInAwt
  @set:CalledInAwt
  var query: GiteePullRequestSearchQuery
    by Delegates.observable(GiteePullRequestSearchQuery(emptyList())) { _, _, _ ->
      stateEventDispatcher.multicaster.queryChanged()
    }

  private val stateEventDispatcher = EventDispatcher.create(StateListener::class.java)

  fun addListener(listener: StateListener, disposable: Disposable) = stateEventDispatcher.addListener(listener, disposable)

  interface StateListener : EventListener {
    fun queryChanged()
  }
}