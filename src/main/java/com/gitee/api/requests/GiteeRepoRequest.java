/*
 * Copyright 2016-2018 码云 - Gitee
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gitee.api.requests;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
public class GiteeRepoRequest {
  @NotNull
  private final String name;
  @NotNull
  private final String description;

  @SerializedName("private") private final boolean isPrivate;

  public GiteeRepoRequest(@NotNull String name, @NotNull String description, boolean aPrivate) {
    this.name = name;
    this.description = description;
    isPrivate = aPrivate;
  }
}
