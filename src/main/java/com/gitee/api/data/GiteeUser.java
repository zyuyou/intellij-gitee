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
public class GiteeUser {
  @NotNull public static final GiteeUser UNKNOWN = createUnknownUser();

  @Mandatory
  private String login;
  private Long id;

  private String url;
  @Mandatory
  private String htmlUrl;

  private Integer followers;
  private Integer following;
  private String avatarUrl;
  private String blog;

  private Date createdAt;

  @NotNull
  public String getLogin() {
    return login;
  }

  @NotNull
  public String getHtmlUrl() {
    return htmlUrl;
  }

  @Nullable
  public String getAvatarUrl() {
    return avatarUrl;
  }

  @NotNull
  private static GiteeUser createUnknownUser() {
    GiteeUser user = new GiteeUser();
    user.login = "ghost";
    return user;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof GiteeUser)) return false;

    GiteeUser user = (GiteeUser) o;
    return id.equals(user.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

}
