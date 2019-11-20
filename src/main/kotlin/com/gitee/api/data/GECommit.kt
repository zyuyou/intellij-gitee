// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data

class GECommit(id: String,
               oid: String,
               abbreviatedOid: String,
               val url: String,
               val messageHeadlineHTML: String,
               val messageBodyHTML: String,
               val author: GEGitActor?)
  : GECommitHash(id, oid, abbreviatedOid)