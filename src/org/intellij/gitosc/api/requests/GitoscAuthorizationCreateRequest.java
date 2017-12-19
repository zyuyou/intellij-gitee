/*
 * Copyright 2016-2017 码云
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
 *
 */
package org.intellij.gitosc.api.requests;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings({"FieldCanBeLocal", "UnusedDeclaration"})
public class GitoscAuthorizationCreateRequest {
  @NotNull
  private final List<String> scopes;

  @Nullable
  private final String note;
  @Nullable
  private final String noteUrl;

  public GitoscAuthorizationCreateRequest(@NotNull List<String> scopes, @Nullable String note, @Nullable String noteUrl) {
    this.scopes = scopes;
    this.note = note;
    this.noteUrl = noteUrl;
  }
}