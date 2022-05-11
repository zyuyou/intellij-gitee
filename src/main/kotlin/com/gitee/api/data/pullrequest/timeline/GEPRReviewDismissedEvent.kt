// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.pullrequest.timeline

import com.fasterxml.jackson.annotation.JsonProperty
import com.gitee.api.data.GEActor
import java.util.*

class GEPRReviewDismissedEvent(override val actor: GEActor?,
                               override val createdAt: Date,
                               val dismissalMessageHTML: String?,
                               @JsonProperty("review") review: ReviewAuthor?)
  : GEPRTimelineEvent.Complex {

  val reviewAuthor = review?.author

  class ReviewAuthor(val author: GEActor?)
}
