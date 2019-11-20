// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.pullrequest

import com.gitee.api.data.GiteeIssueState

enum class GiteePullRequestState {
  CLOSED, MERGED, OPEN;

  fun asIssueState(): GiteeIssueState = if(this == CLOSED || this == MERGED) GiteeIssueState.closed else GiteeIssueState.open
}