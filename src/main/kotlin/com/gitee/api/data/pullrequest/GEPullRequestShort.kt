// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.pullrequest

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.gitee.api.data.*
import java.util.*

open class GEPullRequestShort(id: String,
                              val url: String,
                              val number: Long,
                              val title: String,
                              val state: GiteePullRequestState,
                              val author: GEActor?,
                              val createdAt: Date,
                              @JsonProperty("assignees") assignees: GENodes<GEUser>,
                              @JsonProperty("labels") labels: GENodes<GELabel>) : GENode(id) {

  @JsonIgnore
  val assignees = assignees.nodes
  @JsonIgnore
  val labels = labels.nodes
}
