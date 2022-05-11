// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.pullrequest

import com.gitee.api.data.GENode
import com.intellij.collaboration.api.dto.GraphQLFragment

@GraphQLFragment("/graphql/fragment/teamInfo.graphql")
class GETeam(id: String,
             val slug: String,
             override val url: String,
             override val avatarUrl: String,
             override val name: String?,
             val combinedSlug: String)
  : GENode(id), GEPullRequestRequestedReviewer {
  override val shortName: String = combinedSlug
}