// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.graphql.query

import com.gitee.api.data.graphql.GiteeGQLPageInfo
import com.gitee.api.data.graphql.GiteeGQLPagedRequestResponse

open class GiteeGQLSearchQueryResponse<T>(val search: SearchConnection<T>)
  : GiteeGQLPagedRequestResponse<T> {

  override val pageInfo = search.pageInfo
  override val nodes = search.nodes

  class SearchConnection<T>(val pageInfo: GiteeGQLPageInfo, val nodes: List<T>)
}