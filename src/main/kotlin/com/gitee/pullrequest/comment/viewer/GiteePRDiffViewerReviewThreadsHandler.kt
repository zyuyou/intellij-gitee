// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.comment.viewer

import com.gitee.pullrequest.data.model.GiteePRDiffReviewThreadMapping
import com.intellij.openapi.Disposable

interface GiteePRDiffViewerReviewThreadsHandler : Disposable {
  var mappings: List<GiteePRDiffReviewThreadMapping>
}