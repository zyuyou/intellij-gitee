// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.request;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
public class GiteeAssigneesCollectionRequest {
  @NotNull private final Collection<String> assignees;

  public GiteeAssigneesCollectionRequest(@NotNull Collection<String> assignees) {
    this.assignees = assignees;
  }
}
