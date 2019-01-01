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

import com.gitee.api.GiteeFullPath;
import com.google.gson.annotations.SerializedName;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.io.mandatory.Mandatory;
import org.jetbrains.io.mandatory.RestModel;

import java.util.Objects;

//example/GiteeRepoBasic.json
@RestModel
@SuppressWarnings("UnusedDeclaration")
public class GiteeRepoBasic {
  @Mandatory private Long id;
  //private String nodeId;
  @Mandatory
  private String name;
  private String path;
  private String fullName;
  @Mandatory
  private GiteeUser owner;
  @SerializedName("private")
  @Mandatory
  private Boolean isPrivate;
  @Mandatory
  private String htmlUrl;
  private String description;
  @SerializedName("fork")
  @Mandatory
  private Boolean isFork;

  @Mandatory private String url;
  //urls

  @NotNull
  public String getName() {
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
  public GiteeUser getOwner() {
    return owner;
  }

  @NotNull
  public String getUserName() {
    return getOwner().getLogin();
  }

  @NotNull
  public String getFullName() {
    return StringUtil.isEmptyOrSpaces(fullName) ? getUserName() + "/" + path : fullName;
  }

  @NotNull
  public GiteeFullPath getFullPath() {
    return new GiteeFullPath(getUserName(), path, getFullName());
  }


  @Override
  public String toString() {
    return "GiteeRepo{" +
      "id=" + id +
      ", name='" + name + '\'' +
      '}';
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
