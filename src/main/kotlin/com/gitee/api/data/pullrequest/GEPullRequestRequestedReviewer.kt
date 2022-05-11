// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.pullrequest

//@GraphQLFragment("/graphql/fragment/pullRequestReviewerInfo.graphql")
//@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "__typename", visible = false)
//@JsonSubTypes(
//  JsonSubTypes.Type(name = "User", value = GEUser::class),
//  JsonSubTypes.Type(name = "Team", value = GETeam::class)
//)
interface GEPullRequestRequestedReviewer {
  val shortName: String
  val url: String
  val avatarUrl: String
  val name: String?
}