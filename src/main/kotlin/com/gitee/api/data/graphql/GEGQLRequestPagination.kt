// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.graphql

import com.gitee.api.data.request.GiteeRequestPagination
import java.util.*

class GEGQLRequestPagination private constructor(val afterCursor: String? = null,
                                                 val since: Date? = null,
                                                 val pageSize: Int = GiteeRequestPagination.DEFAULT_PAGE_SIZE) {

  constructor(afterCursor: String? = null,
              pageSize: Int = GiteeRequestPagination.DEFAULT_PAGE_SIZE) : this(afterCursor, null, pageSize)

  constructor(since: Date? = null,
              pageSize: Int = GiteeRequestPagination.DEFAULT_PAGE_SIZE) : this(null, since, pageSize)


  override fun toString(): String {
    return "afterCursor=$afterCursor&since=$since&per_page=$pageSize"
  }
}
