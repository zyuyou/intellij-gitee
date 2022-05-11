// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data

import com.intellij.collaboration.api.dto.GraphQLFragment

@GraphQLFragment("/graphql/fragment/commitShort.graphql")
open class GECommitShort(id: String,
                         oid: String,
                         abbreviatedOid: String,
                         val url: String,
                         val messageHeadlineHTML: String,
                         val author: GEGitActor?)
  : GECommitHash(id, oid, abbreviatedOid) {
}