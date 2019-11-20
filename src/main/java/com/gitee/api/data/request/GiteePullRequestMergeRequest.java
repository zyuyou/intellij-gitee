// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.request;

import com.gitee.api.data.GiteePullRequestMergeMethod;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
public class GiteePullRequestMergeRequest {
  @NotNull private final String commitTitle;
  @NotNull private final String commitMessage;
  @NotNull private final String sha;
  @NotNull private final GiteePullRequestMergeMethod mergeMethod;

  public GiteePullRequestMergeRequest(@NotNull String commitTitle,
                                      @NotNull String commitMessage,
                                      @NotNull String sha,
                                      @NotNull GiteePullRequestMergeMethod mergeMethod) {
    if (mergeMethod != GiteePullRequestMergeMethod.merge && mergeMethod != GiteePullRequestMergeMethod.squash) {
      throw new IllegalArgumentException("Invalid merge method");
    }

    this.commitTitle = commitTitle;
    this.commitMessage = commitMessage;
    this.sha = sha;
    this.mergeMethod = mergeMethod;
  }
}
