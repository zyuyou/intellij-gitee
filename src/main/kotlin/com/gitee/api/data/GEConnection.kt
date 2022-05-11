// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data

import com.intellij.collaboration.api.dto.GraphQLCursorPageInfoDTO
import com.intellij.collaboration.api.dto.GraphQLPagedResponseDataDTO

open class GEConnection<out T>(override val pageInfo: GraphQLCursorPageInfoDTO, nodes: List<T>)
  : GENodes<T>(nodes), GraphQLPagedResponseDataDTO<T>