// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data

import com.fasterxml.jackson.annotation.JsonProperty
import com.intellij.collaboration.api.dto.GraphQLFragment
import com.intellij.openapi.util.NlsSafe

@GraphQLFragment("/graphql/fragment/commit.graphql")
class GECommit(id: String,
               oid: String,
               abbreviatedOid: String,
               url: String,
               @NlsSafe val messageHeadline: String,
               @NlsSafe messageHeadlineHTML: String,
               @NlsSafe val messageBodyHTML: String,
               author: GEGitActor?,
               val committer: GEGitActor?,
               @JsonProperty("parents") parents: GENodes<GECommitHash>)
  : GECommitShort(id, oid, abbreviatedOid, url, messageHeadlineHTML, author) {

  val parents = parents.nodes
}