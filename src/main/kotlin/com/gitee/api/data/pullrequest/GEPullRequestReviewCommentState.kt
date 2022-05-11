// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.pullrequest

enum class GEPullRequestReviewCommentState {
  //A comment that is part of a pending review
  PENDING,

  //A comment that is part of a submitted review
  SUBMITTED
}
