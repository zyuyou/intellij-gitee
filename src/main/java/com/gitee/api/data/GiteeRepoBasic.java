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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.gitee.api.GERepositoryPath;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

@SuppressWarnings("UnusedDeclaration")
public class GiteeRepoBasic {
  private Long id;
  // display [repo_name]
  private String name;
  // [repo_name]
  private String path;
  // [owner]/[repo_name]
  private String fullName;
  // display [owner]/[repo_name]
  private String humanName;

  // api url
  private String url;
  private String htmlUrl;
  private String sshUrl;

  @JsonProperty("private")
  private Boolean isPrivate;
  private String description;
  @JsonProperty("fork")
  private Boolean isFork;

  private GiteeUser owner;

  @NotNull
  public String getName() {
    return path;
  }

  @NotNull
  public String getNickName() {
    return name;
  }

  @NotNull
  public String getDescription() {
    return StringUtil.notNullize(description);
  }

  public boolean isPrivate() {
    return isPrivate;
  }

  public boolean isFork() {
    return isFork;
  }

  @NotNull
  public String getUrl() {
    return url;
  }

  @NotNull
  public String getHtmlUrl() {
    return htmlUrl;
  }

  @NotNull
  public String getSshUrl() {
    return sshUrl;
  }


  @NotNull
  public GiteeUser getOwner() {
    return owner;
  }

  @NotNull
  public String getUserName() {
    return fullName.split("/")[0];
  }

  @NotNull
  public String getHumanUserName() {
    return humanName.split("/")[0];
  }

  @NotNull
  public String getFullName() {
    return fullName;
  }

  @NotNull
  public String getHumanName() {
    return humanName;
  }

  @NotNull
  public GERepositoryPath getFullPath() {
    String[] split = fullName.split("/");
    return new GERepositoryPath(split[0], split[1]);
  }

  public String getId() {
    return id.toString();
  }

  @Override
  public String toString() {
    return "GiteeRepo{"
        + "id=" + id
        + ", path='" + path + '\''
        + ", name='" + name + '\''
        + '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof GiteeRepoBasic)) return false;
    GiteeRepoBasic basic = (GiteeRepoBasic)o;
    return id.equals(basic.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}
