/*
 *  Copyright 2016-2019 码云 - Gitee
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.gitee.api.requests;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
public class GiteeCreateIssueRequest {
  @NotNull
  private final String title;
  @Nullable
  private final String body;
  @Nullable
  private final Long milestone;
  @Nullable
  private final List<String> labels;
  @Nullable
  private final List<String> assignees;

  public GiteeCreateIssueRequest(@NotNull String title,
                                 @Nullable String body,
                                 @Nullable Long milestone,
                                 @Nullable List<String> labels,
                                 @Nullable List<String> assignees) {
    this.title = title;
    this.body = body;
    this.milestone = milestone;
    this.labels = labels;
    this.assignees = assignees;
  }
}
