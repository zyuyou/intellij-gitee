// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.pullrequest

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.gitee.api.data.GEActor
import com.gitee.api.data.GELabel
import com.gitee.api.data.GENodes
import com.gitee.api.data.GEUser
import java.util.*

class GEPullRequest(id: String,
                    url: String,
                    number: Long,
                    title: String,
                    state: GiteePullRequestState,
                    author: GEActor?,
                    createdAt: Date,
                    @JsonProperty("assignees") assignees: GENodes<GEUser>,
                    @JsonProperty("labels") labels: GENodes<GELabel>,
                    val bodyHTML: String,
                    val mergeable: GiteePullRequestMergeableState,
                    @JsonProperty("reviewRequests") reviewRequests: GENodes<GiteePullRequestReviewRequest>,
                    val baseRefName: String,
                    val baseRefOid: String,
                    headRefName: String,
                    val headRefOid: String,
                    headRepository: Repository?,
                    val viewerCanUpdate: Boolean,
                    val viewerDidAuthor: Boolean)
  : GEPullRequestShort(id, url, number, title, state, author, createdAt, assignees, labels) {

  @JsonIgnore
  val reviewRequests = reviewRequests.nodes
  @JsonIgnore
  val headLabel = headRepository?.nameWithOwner.orEmpty() + ":" + headRefName

  class Repository(val nameWithOwner: String)
}
