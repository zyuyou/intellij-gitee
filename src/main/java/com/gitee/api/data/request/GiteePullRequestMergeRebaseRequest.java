// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.request;

import com.gitee.api.data.GiteePullRequestMergeMethod;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
public class GiteePullRequestMergeRebaseRequest {
  @NotNull private final String sha;
  @NotNull private final GiteePullRequestMergeMethod mergeMethod;

  public GiteePullRequestMergeRebaseRequest(@NotNull String sha) {
    this.sha = sha;
    this.mergeMethod = GiteePullRequestMergeMethod.rebase;
  }
}
