// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data

import com.gitee.api.data.graphql.GiteeGQLPageInfo
import com.gitee.api.data.graphql.GiteeGQLPagedRequestResponse

open class GEConnection<out T>(override val pageInfo: GiteeGQLPageInfo, nodes: List<T>)
  : GENodes<T>(nodes), GiteeGQLPagedRequestResponse<T>