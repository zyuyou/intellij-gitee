// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.graphql

import com.gitee.api.data.request.GiteeRequestPagination

class GiteeGQLRequestPagination @JvmOverloads constructor(val afterCursor: String? = null,
                                                          val pageSize: Int = GiteeRequestPagination.DEFAULT_PAGE_SIZE) {
  override fun toString(): String {
    return "afterCursor=$afterCursor&per_page=$pageSize"
  }
}
