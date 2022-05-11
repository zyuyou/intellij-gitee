// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.request.query

import com.gitee.api.data.GEApiCursorPageInfoDTO
import com.gitee.api.data.GEApiPagedResponseDataDTO

open class GEApiSearchQueryResponse<T>(val search: SearchConnection<T>)
  : GEApiPagedResponseDataDTO<T> {

  override val pageInfo = search.pageInfo
  override val items = search.items

  class SearchConnection<T>(val pageInfo: GEApiCursorPageInfoDTO, val items: List<T>)
}