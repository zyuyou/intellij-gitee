// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data

import java.util.*

open class GEComment(id: String,
                     val author: GEActor?,
                     val bodyHtml: String,
                     val createdAt: Date)
  : GENode(id)
