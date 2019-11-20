// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.request;

import com.gitee.api.data.GiteeIssueState;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
public class GiteePullUpdateRequest {
  @Nullable private final String title;
  @Nullable private final String body;
  @Nullable private final GiteeIssueState state;
  @Nullable private final String base;
  @Nullable private final Boolean maintainerCanModify;

  public GiteePullUpdateRequest(@Nullable String title,
                                @Nullable String body,
                                @Nullable GiteeIssueState state,
                                @Nullable String base,
                                @Nullable Boolean maintainerCanModify) {
    this.title = title;
    this.body = body;
    this.state = state;
    this.base = base;
    this.maintainerCanModify = maintainerCanModify;
  }
}
