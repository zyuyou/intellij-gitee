// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.pullrequest

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.gitee.api.data.GEUser

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "__typename", visible = false)
@JsonSubTypes(
  JsonSubTypes.Type(name = "User", value = GEUser::class),
  JsonSubTypes.Type(name = "Team", value = GiteePullRequestReviewer.Team::class)
)
interface GiteePullRequestReviewer {
  // because we need scopes to access teams
  class Team : GiteePullRequestReviewer {
    override fun toString(): String {
      return "Unknown Team"
    }
  }
}