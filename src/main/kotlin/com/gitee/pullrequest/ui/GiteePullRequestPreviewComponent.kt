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

import com.gitee.pullrequest.data.GiteePullRequestDataProvider
import com.intellij.openapi.Disposable
import com.intellij.ui.OnePixelSplitter

/**
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/pullrequest/ui/GithubPullRequestPreviewComponent.kt
 * @author JetBrains s.r.o.
 */
internal class GiteePullRequestPreviewComponent(private val changes: GiteePullRequestChangesComponent,
                                                private val details: GiteePullRequestDetailsComponent)
  : OnePixelSplitter("Gitee.PullRequest.Preview.Component", 0.5f), Disposable {

  init {
    firstComponent = details
    secondComponent = changes
  }

  fun setPreviewDataProvider(provider: GiteePullRequestDataProvider?) {
    changes.loadAndShow(provider?.changesRequest)
    details.loadAndShow(provider?.detailsRequest)
  }

  override fun dispose() {}
}
