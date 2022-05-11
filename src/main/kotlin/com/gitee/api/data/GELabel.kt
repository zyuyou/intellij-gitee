// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data

import com.google.common.annotations.VisibleForTesting
import com.intellij.collaboration.api.dto.GraphQLFragment
import com.intellij.openapi.util.NlsSafe

@GraphQLFragment("/graphql/fragment/labelInfo.graphql")
class GELabel(id: String,
              val url: String,
              @NlsSafe val name: String,
              val color: String)
  : GENode(id) {

  companion object {
    @VisibleForTesting
    internal fun createTest(id: String) = GELabel(id, "", "testLabel_$id", "#FFFFFF")
  }
}