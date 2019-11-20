// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.request;

import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
public class GiteeChangeIssueStateRequest {
  @NotNull private final String state;

  public GiteeChangeIssueStateRequest(@NotNull String state) {
    this.state = state;
  }
}
