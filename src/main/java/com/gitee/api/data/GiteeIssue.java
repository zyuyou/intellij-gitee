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

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.mandatory.Mandatory;
import org.jetbrains.io.mandatory.RestModel;

import java.util.Date;
import java.util.List;

@RestModel
@SuppressWarnings("UnusedDeclaration")
public class GiteeIssue {
  private String url;
  @Mandatory
  private String htmlUrl;
  @Mandatory
  private String commentsUrl;
  @Mandatory
  private String number;
  @Mandatory
  private GiteeIssueState state;
  @Mandatory
  private String title;
  private String body;

  @Mandatory
  private GiteeUser user;
  private GiteeUser assignee;
  @Mandatory
  private List<GiteeUser> collaborators;

  @Mandatory
  private List<GiteeIssueLabel> labels;

  private Date closedAt;
  @Mandatory
  private Date createdAt;
  @Mandatory
  private Date updatedAt;

  @NotNull
  public String getUrl() {
    return url;
  }

  @NotNull
  public String getHtmlUrl() {
    return htmlUrl;
  }

  @NotNull
  public String getCommentsUrl() {
    return commentsUrl;
  }

  public String getNumber() {
    return number;
  }

  @NotNull
  public GiteeIssueState getState() {
    return state;
  }

  @NotNull
  public String getTitle() {
    return title;
  }

  @NotNull
  public String getBody() {
    return StringUtil.notNullize(body);
  }

  @NotNull
  public GiteeUser getUser() {
    return user;
  }

  @Nullable
  public GiteeUser getAssignee() {
    return assignee;
  }

  @NotNull
  public List<GiteeUser> getCollaborators() {
    return collaborators;
  }

  @NotNull
  public List<GiteeIssueLabel> getLabels() {
    return labels;
  }

  @Nullable
  public Date getClosedAt() {
    return closedAt;
  }

  @NotNull
  public Date getCreatedAt() {
    return createdAt;
  }

  @NotNull
  public Date getUpdatedAt() {
    return updatedAt;
  }
}
