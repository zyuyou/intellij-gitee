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

import com.gitee.api.data.GiteePullRequestMergeMethod;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
public class GiteePullRequestMergeRequest {
  @NotNull
  private final String commitTitle;
  @NotNull
  private final String commitMessage;
  @NotNull
  private final String sha;
  @NotNull
  private final GiteePullRequestMergeMethod method;

  public GiteePullRequestMergeRequest(@NotNull String commitTitle,
                                      @NotNull String commitMessage,
                                      @NotNull String sha,
                                      @NotNull GiteePullRequestMergeMethod method) {
    if (method != GiteePullRequestMergeMethod.merge && method != GiteePullRequestMergeMethod.squash) {
      throw new IllegalArgumentException("Invalid merge method");
    }

    this.commitTitle = commitTitle;
    this.commitMessage = commitMessage;
    this.sha = sha;
    this.method = method;
  }
}
