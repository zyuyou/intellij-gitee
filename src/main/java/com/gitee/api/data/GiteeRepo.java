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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.mandatory.Mandatory;
import org.jetbrains.io.mandatory.RestModel;

import java.util.Date;

@RestModel
@SuppressWarnings("UnusedDeclaration")
public class GiteeRepo extends GiteeRepoBasic {
  private Date createdAt;
  private Date updatedAt;
  private Date pushedAt;

//  private String path;

//  private String forksUrl;
//  private String keysUrl;
//  private String collaboratorsUrl;
//  private String hooksUrl;
//  private String branchesUrl;
//  private String tagsUrl;
//  private String blobsUrl;
//  private String stargazersUrl;
//  private String contributorsUrl;
//  private String commitsUrl;
//  private String commentsUrl;
//  private String issueCommentUrl;
//  private String issuesUrl;
//  private String pullsUrl;
//  private String milestonesUrl;
//  private String notificationsUrl;
//  private String labelsUrl;
//  private String releasesUrl;

//  private Boolean recommand;

  private String homepage;
  private String language;

  private Integer forksCount;
  private Integer stargazersCount;
  private Integer watchersCount;
  private Integer openIssuesCount;

//  private String masterBranch;
  private String defaultBranch;

  private Boolean hasIssues;
  private Boolean hasWiki;
  private Boolean hasDownloads;
  private Boolean hasPage;

//  private Boolean pullRequestsEnabled;
//  private String license;

  private Boolean stared;
  private Boolean watched;

  @Nullable
  public String getDefaultBranch() {
    return defaultBranch;
  }

}
