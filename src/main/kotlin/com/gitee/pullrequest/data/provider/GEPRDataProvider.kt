// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data.provider

import com.gitee.api.data.pullrequest.timeline.GEPRTimelineItem
import com.gitee.pullrequest.GEPRDiffRequestModel
import com.gitee.pullrequest.data.GEListLoader
import com.gitee.pullrequest.data.GEPRIdentifier
import com.intellij.openapi.Disposable
import com.intellij.util.concurrency.annotations.RequiresEdt

interface GEPRDataProvider {
  val id: GEPRIdentifier
  val detailsData: GEPRDetailsDataProvider
  val stateData: GEPRStateDataProvider
  val changesData: GEPRChangesDataProvider
  val commentsData: GEPRCommentsDataProvider
  val reviewData: GEPRReviewDataProvider
  val viewedStateData: GEPRViewedStateDataProvider
  val timelineLoader: GEListLoader<GEPRTimelineItem>?
  val diffRequestModel: GEPRDiffRequestModel

  @RequiresEdt
  fun acquireTimelineLoader(disposable: Disposable): GEListLoader<GEPRTimelineItem>
}