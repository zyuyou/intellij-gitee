// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.graphql

class GiteeGQLPageInfo(val startCursor: String?, val hasPreviousPage: Boolean,
                       val endCursor: String?, val hasNextPage: Boolean)