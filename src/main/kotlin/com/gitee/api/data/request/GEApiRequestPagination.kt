/*
 *  Copyright 2016-2019 码云 - Gitee
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.gitee.api.data.request

import java.util.*

class GEApiRequestPagination private constructor(val page: Int? = 1,
                                                 val since: Date? = null,
                                                 val pageSize: Int = GiteeRequestPagination.DEFAULT_PAGE_SIZE) {

  constructor(page: Int? = 1,
              pageSize: Int = GiteeRequestPagination.DEFAULT_PAGE_SIZE) : this(page, null, pageSize)

  constructor(since: Date? = null,
              pageSize: Int = GiteeRequestPagination.DEFAULT_PAGE_SIZE) : this(1, since, pageSize)

  override fun toString(): String {
    return "page=$page&per_page=$pageSize"
  }
}