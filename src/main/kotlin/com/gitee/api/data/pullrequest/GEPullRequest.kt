// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.pullrequest

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.gitee.api.data.GEActor
import com.gitee.api.data.GELabel
import com.gitee.api.data.GENodes
import com.gitee.api.data.GEUser
import com.intellij.collaboration.api.dto.GraphQLFragment
import com.intellij.openapi.util.NlsSafe
import java.util.*

@GraphQLFragment("/graphql/fragment/pullRequestInfo.graphql")
class GEPullRequest(id: String,
                    url: String,
                    number: Long,
                    title: String,
                    state: GEPullRequestState,
                    isDraft: Boolean,
                    author: GEActor?,
                    createdAt: Date,
                    @JsonProperty("assignees") assignees: GENodes<GEUser>,
                    @JsonProperty("labels") labels: GENodes<GELabel>,
                    viewerCanUpdate: Boolean,
                    viewerDidAuthor: Boolean,
                    @NlsSafe val body: String,
                    @JsonProperty("reviewRequests") reviewRequests: GENodes<GEPullRequestReviewRequest>,
                    val baseRefName: String,
                    val baseRefOid: String,
                    val baseRepository: Repository?,
                    val headRefName: String,
                    val headRefOid: String,
                    val headRepository: HeadRepository?)
  : GEPullRequestShort(id, url, number, title, state, isDraft, author, createdAt, assignees, labels, viewerCanUpdate, viewerDidAuthor) {

  @JsonIgnore
  val reviewRequests = reviewRequests.nodes

  open class Repository(val owner: Owner, val isFork: Boolean)

  class HeadRepository(owner: Owner, isFork: Boolean, val url: String, val sshUrl: String) : Repository(owner, isFork)

  class Owner(val login: String)
}