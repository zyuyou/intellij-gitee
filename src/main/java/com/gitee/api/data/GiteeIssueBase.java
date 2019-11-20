// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("UnusedDeclaration")
public abstract class GiteeIssueBase {
  private String url;
  private String htmlUrl;
  private String commentsUrl;
  private String number;
  private GiteeIssueState state;
  private String title;
  private String body;

  private GiteeUser user;
  private GiteeUser assignee;
  private List<GiteeUser> collaborators;

  private List<GiteeIssueLabel> labels;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", locale = "zh", timezone = "GMT+8")
  private Date finishedAt;
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", locale = "zh", timezone = "GMT+8")
  private Date createdAt;
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", locale = "zh", timezone = "GMT+8")
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
  public Date getFinishedAt() {
    return finishedAt;
  }

  @NotNull
  public Date getCreatedAt() {
    return createdAt;
  }

  @NotNull
  public Date getUpdatedAt() {
    return updatedAt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof GiteeIssueBase)) return false;
    GiteeIssueBase base = (GiteeIssueBase)o;
    return number.equals(base.number);
  }

  @Override
  public int hashCode() {
    return Objects.hash(number);
  }
}
