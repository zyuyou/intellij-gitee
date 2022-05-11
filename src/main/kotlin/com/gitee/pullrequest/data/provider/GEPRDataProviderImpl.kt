// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data.provider

import com.gitee.api.data.pullrequest.timeline.GEPRTimelineItem
import com.gitee.pullrequest.GEPRDiffRequestModel
import com.gitee.pullrequest.data.GEListLoader
import com.gitee.pullrequest.data.GEPRIdentifier
import com.gitee.util.DisposalCountingHolder
import com.intellij.openapi.Disposable

internal class GEPRDataProviderImpl(override val id: GEPRIdentifier,
                                    override val detailsData: GEPRDetailsDataProvider,
                                    override val stateData: GEPRStateDataProvider,
                                    override val changesData: GEPRChangesDataProvider,
                                    override val commentsData: GEPRCommentsDataProvider,
                                    override val reviewData: GEPRReviewDataProvider,
                                    override val viewedStateData: GEPRViewedStateDataProvider,
                                    private val timelineLoaderHolder: DisposalCountingHolder<GEListLoader<GEPRTimelineItem>>,
                                    override val diffRequestModel: GEPRDiffRequestModel
)
  : GEPRDataProvider {

  override val timelineLoader get() = timelineLoaderHolder.value

  override fun acquireTimelineLoader(disposable: Disposable) =
    timelineLoaderHolder.acquireValue(disposable)
}