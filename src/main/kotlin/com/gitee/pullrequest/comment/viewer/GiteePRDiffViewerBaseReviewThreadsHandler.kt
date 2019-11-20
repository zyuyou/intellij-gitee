// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.comment.viewer

import com.gitee.pullrequest.comment.ui.GiteePREditorReviewThreadComponentFactory
import com.gitee.pullrequest.data.model.GiteePRDiffReviewThreadMapping
import com.intellij.diff.tools.util.base.DiffViewerBase
import org.jetbrains.annotations.CalledInAwt
import kotlin.properties.Delegates.observable

abstract class GiteePRDiffViewerBaseReviewThreadsHandler<T : DiffViewerBase>(protected val viewer: T,
                                                                             protected val componentFactory: GiteePREditorReviewThreadComponentFactory)
  : GiteePRDiffViewerReviewThreadsHandler {

  protected abstract val viewerReady: Boolean

  override var mappings by observable<List<GiteePRDiffReviewThreadMapping>>(emptyList()) { _, _, newValue ->
    if (viewerReady) updateThreads(newValue)
  }

  @CalledInAwt
  abstract fun updateThreads(mappings: List<GiteePRDiffReviewThreadMapping>)

  override fun dispose() {}
}