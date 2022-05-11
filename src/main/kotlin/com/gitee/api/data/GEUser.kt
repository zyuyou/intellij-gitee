// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data

import com.gitee.api.data.pullrequest.GEPullRequestRequestedReviewer
import com.intellij.collaboration.api.dto.GraphQLFragment
import com.intellij.openapi.util.NlsSafe

@GraphQLFragment("/graphql/fragment/userInfo.graphql")
class GEUser(id: String,
             @NlsSafe override val login: String,
             override val url: String,
             override val avatarUrl: String,
             @NlsSafe override val name: String?)
  : GENode(id), GEActor, GEPullRequestRequestedReviewer {
  override val shortName: String = login
}
