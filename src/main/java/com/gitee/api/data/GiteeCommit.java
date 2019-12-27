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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.mandatory.RestModel;

import java.util.Date;

@RestModel
@SuppressWarnings("UnusedDeclaration")
public class GiteeCommit extends GiteeCommitSha {
  private GiteeUser author;
  private GiteeUser committer;

  private GitCommit commit;

//  private List<GiteeCommitSha> parents;

  @RestModel
  public static class GitCommit {
    private String url;
    private String message;
    private GitUser author;
    private GitUser committer;

    @NotNull
    public String getMessage() {
      return message;
    }

    @NotNull
    public GitUser getAuthor() {
      return author;
    }

    @NotNull
    public GitUser getCommitter() {
      return committer;
    }
  }

  @RestModel
  public static class GitUser {
    private String name;
    private String email;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", locale = "zh", timezone = "GMT+8")
    private Date date;

    @NotNull
    public String getName() {
      return name;
    }

    @NotNull
    public String getEmail() {
      return email;
    }

    @NotNull
    public Date getDate() {
      return date;
    }
  }

  @Nullable
  public GiteeUser getAuthor() {
    return author;
  }

  @Nullable
  public GiteeUser getCommitter() {
    return committer;
  }

//  @NotNull
//  public List<GiteeCommitSha> getParents() {
//    return parents;
//  }

  @NotNull
  public GitCommit getCommit() {
    return commit;
  }
}
