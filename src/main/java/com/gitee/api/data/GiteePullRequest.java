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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gitee.api.data.util.GiteeLink;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.mandatory.Mandatory;
import org.jetbrains.io.mandatory.RestModel;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class GiteePullRequest {
  private long id;
  private String url;
  private long number;
  private String title;
  private GiteeIssueState state;
  private GiteeUser user;

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", locale = "zh", timezone = "GMT+8")
  private Date createdAt;
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", locale = "zh", timezone = "GMT+8")
  private Date updatedAt;
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", locale = "zh", timezone = "GMT+8")
  private Date closedAt;
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", locale = "zh", timezone = "GMT+8")
  private Date mergedAt;

  private List<GiteeUser> assignees; // 审查人员
  private List<GiteeUser> testers;   // 测试人员

  private List<GiteeIssueLabel> labels;

  private String body;

  private String htmlUrl;
  private String diffUrl;
  private String patchUrl;
  private String issueUrl;

  @Mandatory
  private Links _links;

  @NotNull
  public String getUrl() {
    return url;
  }

  public long getId() {
    return id;
  }

  public long getNumber() {
    return number;
  }

  @NotNull
  public String getTitle() {
    return title;
  }

  @NotNull
  public GiteeIssueState getState() {
    return state;
  }

  @NotNull
  public Date getCreatedAt() {
    return createdAt;
  }

  @NotNull
  public Date getUpdatedAt() {
    return updatedAt;
  }

  @Nullable
  public Date getClosedAt() {
    return closedAt;
  }

  @Nullable
  public Date getMergedAt() {
    return mergedAt;
  }

  @NotNull
  public GiteeUser getUser() {
    return ObjectUtils.notNull(user, GiteeUser.UNKNOWN);
  }

  @NotNull
  public List<GiteeUser> getAssignees() {
    return assignees;
  }

  @NotNull
  public List<GiteeUser> getTesters() {
    return testers;
  }

  @NotNull
  public List<GiteeIssueLabel> getLabels() {
    return Objects.requireNonNullElse(labels, Collections.emptyList());
  }

  @NotNull
  public String getHtmlUrl() {
    return htmlUrl;
  }

  @NotNull
  public String getDiffUrl() {
    return diffUrl;
  }

  @NotNull
  public String getPatchUrl() {
    return patchUrl;
  }

  @NotNull
  public String getBody() {
    return body;
  }

  @NotNull
  public Links getLinks() {
    return _links;
  }

  @RestModel
  public static class Tag {
    @Mandatory
    private String label;
    @Mandatory
    private String ref;
    @Mandatory
    private String sha;

    private GiteeRepo repo;
    private GiteeUser user;

    @NotNull
    public String getLabel() {
      return label;
    }

    @NotNull
    public String getRef() {
      return ref;
    }

    @NotNull
    public String getSha() {
      return sha;
    }

    @Nullable
    public GiteeRepo getRepo() {
      return repo;
    }

    @Nullable
    public GiteeUser getUser() {
      return user;
    }
  }

  @RestModel
  public static class Links {
    @Mandatory
    private GiteeLink self;
    @Mandatory
    private GiteeLink html;
    @Mandatory
    private GiteeLink issue;
    @Mandatory
    private GiteeLink comments;
    @Mandatory
    private GiteeLink reviewComments;
    @Mandatory
    private GiteeLink reviewComment;
    @Mandatory
    private GiteeLink commits;
    @Mandatory
    private GiteeLink statuses;
  }
}
