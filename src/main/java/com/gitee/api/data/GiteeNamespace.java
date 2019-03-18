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
package com.gitee.api.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.mandatory.Mandatory;
import org.jetbrains.io.mandatory.RestModel;

import java.util.Date;
import java.util.Objects;

@RestModel
@SuppressWarnings("UnusedDeclaration")
public class GiteeNamespace {
  @Mandatory
  private Long id;
  @Mandatory
  private GiteeNamespaceType type;
  @Mandatory
  private String name;
  @Mandatory
  private String path;
  @Mandatory
  private String htmlUrl;

  @NotNull
  public GiteeNamespaceType getType() {
    return type;
  }

  @NotNull
  public String getName() {
    return name;
  }

  @NotNull
  public String getPath() {
    return path;
  }

  @NotNull
  public String getHtmlUrl() {
    return htmlUrl;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof GiteeNamespace)) return false;

    GiteeNamespace namespace = (GiteeNamespace) o;
    return id.equals(namespace.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

}
