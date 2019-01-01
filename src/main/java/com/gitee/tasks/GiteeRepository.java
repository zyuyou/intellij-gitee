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
package com.gitee.tasks;

import com.gitee.api.GiteeApiRequestExecutor;
import com.gitee.api.GiteeApiRequests;
import com.gitee.api.GiteeServerPath;
import com.gitee.api.data.GiteeAuthorization;
import com.gitee.api.data.GiteeIssue;
import com.gitee.api.data.GiteeIssueComment;
import com.gitee.api.data.GiteeIssueState;
import com.gitee.api.util.GiteeApiPagesLoader;
import com.gitee.authentication.accounts.GiteeAccountManager;
import com.gitee.authentication.util.GiteeTokenCreator;
import com.gitee.exceptions.GiteeAuthenticationException;
import com.gitee.exceptions.GiteeJsonException;
import com.gitee.exceptions.GiteeRateLimitExceededException;
import com.gitee.exceptions.GiteeStatusCodeException;
import com.gitee.icons.GiteeIcons;
import com.gitee.issue.GiteeIssuesLoadingHelper;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.*;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.PasswordUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.tasks.*;
import com.intellij.tasks.Task;
import com.intellij.tasks.impl.BaseRepository;
import com.intellij.tasks.impl.BaseRepositoryImpl;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.Transient;
import kotlin.Pair;
import kotlin.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/tasks/GithubRepository.java
 * @author Dennis.Ushakov
 */
@Tag("Gitee")
public class GiteeRepository extends BaseRepositoryImpl {
  private static final Logger LOG = Logger.getInstance(GiteeRepository.class);

  private Pattern myPattern = Pattern.compile("($^)");
  @NotNull
  private String myRepoAuthor = "";
  @NotNull
  private String myRepoName = "";
  @NotNull
  private String myUser = "";
  @NotNull
  private String myAccessToken = "";
  @NotNull
  private String myRefreshToken = "";

  private boolean myAssignedIssuesOnly = false;

  @SuppressWarnings({"UnusedDeclaration"})
  public GiteeRepository() {
  }

  public GiteeRepository(GiteeRepository other) {
    super(other);
    setRepoName(other.myRepoName);
    setRepoAuthor(other.myRepoAuthor);
    setTokens(other.myAccessToken, other.myRefreshToken);
    setAssignedIssuesOnly(other.myAssignedIssuesOnly);
  }

  public GiteeRepository(GiteeRepositoryType type) {
    super(type);
    setUrl("https://" + GiteeServerPath.DEFAULT_HOST);
  }

  @NotNull
  @Override
  public CancellableConnection createCancellableConnection() {
    return new CancellableConnection() {
      private final GiteeApiRequestExecutor myExecutor = getExecutor();
      private final ProgressIndicator myIndicator = new EmptyProgressIndicator();

      @Override
      protected void doTest() throws Exception {
        try {
          myExecutor.execute(myIndicator, GiteeApiRequests.Repos.get(getServer(), getRepoAuthor(), getRepoName()));
        } catch (ProcessCanceledException ignore) {
        }
      }

      @Override
      public void cancel() {
        myIndicator.cancel();
      }
    };
  }

  @Override
  public boolean isConfigured() {
    return super.isConfigured() && !StringUtil.isEmptyOrSpaces(getRepoAuthor()) && !StringUtil.isEmptyOrSpaces(getRepoName()) && !StringUtil.isEmptyOrSpaces(getAccessToken());
  }

  @Override
  public String getPresentableName() {
    final String name = super.getPresentableName();
    return name + (!StringUtil.isEmpty(getRepoAuthor()) ? "/" + getRepoAuthor() : "") + (!StringUtil.isEmpty(getRepoName()) ? "/" + getRepoName() : "");
  }

  @Override
  public Task[] getIssues(@Nullable String query, int offset, int limit, boolean withClosed) throws Exception {
    try {
      return getIssues(query, offset + limit, withClosed);
    } catch (GiteeRateLimitExceededException e) {
      return Task.EMPTY_ARRAY;
    } catch (GiteeAuthenticationException | GiteeStatusCodeException e) {
      throw new Exception(e.getMessage(), e); // Wrap to show error message
    } catch (GiteeJsonException e) {
      throw new Exception("Bad response format", e);
    }
  }

  @Override
  public Task[] getIssues(@Nullable String query, int offset, int limit, boolean withClosed, @NotNull ProgressIndicator cancelled) throws Exception {
    return getIssues(query, offset, limit, withClosed);
  }

  @NotNull
  private Task[] getIssues(@Nullable String query, int max, boolean withClosed) throws Exception {
    GiteeApiRequestExecutor executor = getExecutor();
    ProgressIndicator indicator = getProgressIndicator();
    GiteeServerPath server = getServer();

    String assigned = null;
    if (myAssignedIssuesOnly) {
      if (StringUtil.isEmptyOrSpaces(myUser)) {
        myUser = executor.execute(indicator, GiteeApiRequests.CurrentUser.get(server)).getLogin();
      }
      assigned = myUser;
    }

    List<GiteeIssue> issues;

    if (StringUtil.isEmptyOrSpaces(query)) {
      // search queries have way smaller request number limit
      issues = GiteeIssuesLoadingHelper.load(executor, indicator, server, getRepoAuthor(), getRepoName(), withClosed, max, assigned);
    } else {
      issues = Collections.emptyList();
//			issues = GiteeIssuesLoadingHelper.search(executor, indicator, server, getRepoAuthor(), getRepoName(), withClosed, assigned, query);
    }
//    return ContainerUtil.map2Array(issues, Task.class, this::createTask);

    List<Task> tasks = new ArrayList<>();

    for (GiteeIssue issue : issues) {
      List<GiteeIssueComment> comments = GiteeApiPagesLoader
        .loadAll(executor, indicator, GiteeApiRequests.Repos.Issues.Comments.pages(issue.getCommentsUrl()));
      tasks.add(createTask(issue, comments));
    }

    return tasks.toArray(Task.EMPTY_ARRAY);

  }

  @NotNull
  private Task createTask(@NotNull GiteeIssue issue, @NotNull List<GiteeIssueComment> comments) {
    return new Task() {
      @NotNull
      private final String myRepoName = getRepoName();

      @NotNull
      private final Comment[] myComments =
        ContainerUtil.map2Array(comments, Comment.class,
          comment -> new GiteeComment(
            comment.getCreatedAt(),
            comment.getUser().getLogin(),
            comment.getBody(),
            comment.getUser().getAvatarUrl(),
            comment.getUser().getHtmlUrl())
        );

      @Override
      public boolean isIssue() {
        return true;
      }

      @Override
      public String getIssueUrl() {
        return issue.getHtmlUrl();
      }

      @NotNull
      @Override
      public String getId() {
        return myRepoName + "-" + issue.getNumber();
      }

      @NotNull
      @Override
      public String getSummary() {
        return issue.getTitle();
      }

      @Override
      public String getDescription() {
        return issue.getBody();
      }

      @NotNull
      @Override
      public Comment[] getComments() {
//        try {
//          return fetchComments(issue.getNumber());
//        } catch (Exception e) {
//          LOG.warn("Error fetching comments for " + issue.getNumber(), e);
//          return Comment.EMPTY_ARRAY;
//        }
        return myComments;
      }

      @NotNull
      @Override
      public Icon getIcon() {
        return GiteeIcons.Gitee_icon;
      }

      @NotNull
      @Override
      public TaskType getType() {
        return TaskType.BUG;
      }

      @Override
      public Date getUpdated() {
        return issue.getUpdatedAt();
      }

      @Override
      public Date getCreated() {
        return issue.getCreatedAt();
      }

      @Override
      public boolean isClosed() {
        return !GiteeIssueState.open.equals(issue.getState());
//				return !"open".equals(issue.getState());
      }

      @Override
      public TaskRepository getRepository() {
        return GiteeRepository.this;
      }

      @Override
      public String getPresentableName() {
        return getId() + ": " + getSummary();
      }
    };
  }

//  private Comment[] fetchComments(final String id) throws Exception {
//    GiteeApiRequestExecutor executor = getExecutor();
//    ProgressIndicator indicator = getProgressIndicator();
//
//    List<GiteeIssueComment> result = GiteeApiPagesLoader.loadAll(
//      executor, indicator, GiteeApiRequests.Repos.Issues.Comments.pages(getServer(), getRepoAuthor(), getRepoName(), id)
//    );
//
//    return ContainerUtil.map2Array(result, Comment.class, comment -> new GiteeComment(comment.getCreatedAt(), comment.getUser().getLogin(), comment.getBody(), comment.getUser().getAvatarUrl(), comment.getUser().getHtmlUrl()));
//  }

  @Override
  @Nullable
  public String extractId(@NotNull String taskName) {
    Matcher matcher = myPattern.matcher(taskName);
    return matcher.find() ? matcher.group(1) : null;
  }

  @Nullable
  @Override
  public Task findTask(@NotNull String id) throws Exception {
    final int index = id.lastIndexOf("-");
    if (index < 0) {
      return null;
    }
    final String numericId = id.substring(index + 1);
    GiteeApiRequestExecutor executor = getExecutor();
    ProgressIndicator indicator = getProgressIndicator();
    GiteeIssue issue = executor.execute(indicator, GiteeApiRequests.Repos.Issues.get(getServer(), getRepoAuthor(), getRepoName(), numericId));
    if (issue == null) return null;

    List<GiteeIssueComment> comments = GiteeApiPagesLoader
      .loadAll(executor, indicator, GiteeApiRequests.Repos.Issues.Comments.pages(issue.getCommentsUrl()));
    return createTask(issue, comments);
  }

  @Override
  public void setTaskState(@NotNull Task task, @NotNull TaskState state) throws Exception {
    boolean isOpen;
    switch (state) {
      case OPEN:
        isOpen = true;
        break;
      case RESOLVED:
        isOpen = false;
        break;
      default:
        throw new IllegalStateException("Unknown state: " + state);
    }
    GiteeApiRequestExecutor executor = getExecutor();
    GiteeServerPath server = getServer();
    String repoAuthor = getRepoAuthor();
    String repoName = getRepoName();

    ProgressIndicator indicator = getProgressIndicator();
    executor.execute(indicator, GiteeApiRequests.Repos.Issues.updateState(server, repoAuthor, repoName, task.getNumber(), isOpen));
  }

  @NotNull
  @Override
  public BaseRepository clone() {
    return new GiteeRepository(this);
  }

  @NotNull
  public String getRepoName() {
    return myRepoName;
  }

  public void setRepoName(@NotNull String repoName) {
    myRepoName = repoName;
    myPattern = Pattern.compile("(" + StringUtil.escapeToRegexp(repoName) + "-\\d+)");
  }

  @NotNull
  public String getRepoAuthor() {
    return myRepoAuthor;
  }

  public void setRepoAuthor(@NotNull String repoAuthor) {
    myRepoAuthor = repoAuthor;
  }

  @NotNull
  public String getUser() {
    return myUser;
  }

  public void setUser(@NotNull String user) {
    myUser = user;
  }

  @Transient
  @NotNull
  public String getAccessToken() {
    return myAccessToken;
  }

  @Transient
  @NotNull
  public String getRefreshToken() {
    return myRefreshToken;
  }

  public void setTokens(@NotNull String accessToken, @NotNull String refreshToken) {
    myAccessToken = accessToken;
    myRefreshToken = refreshToken;
    setUser("");
  }

  public boolean isAssignedIssuesOnly() {
    return myAssignedIssuesOnly;
  }

  public void setAssignedIssuesOnly(boolean value) {
    myAssignedIssuesOnly = value;
  }

  @Tag("token")
  public String getEncodedToken() {
    return PasswordUtil.encodePassword(getAccessToken() + "&" + getRefreshToken());
  }

  public void setEncodedToken(String password) {
    try {
      String[] tokenList = PasswordUtil.decodePassword(password).split("&");

      if (tokenList.length == 1) {
        setTokens(tokenList[0], "");
      } else {
        setTokens(tokenList[0], tokenList[1]);
      }
    } catch (NumberFormatException e) {
      LOG.warn("Can't decode token", e);
    }
  }

  @NotNull
  private GiteeApiRequestExecutor getExecutor() {
    return GiteeApiRequestExecutor.Factory.getInstance().create(new Pair<>(getAccessToken(), getRefreshToken()), (refreshToken) -> {
        GiteeAuthorization authorization;

        try {
          authorization = new GiteeTokenCreator(
            getServer(),
            GiteeApiRequestExecutor.Factory.getInstance().create(),
            new DumbProgressIndicator()
          ).updateMaster(refreshToken);

          myAccessToken = authorization.getAccessToken();
          myRefreshToken = authorization.getRefreshToken();

          return new Triple<>(GiteeAccountManager.Companion.createAccount(getUser(), getServer()), getAccessToken(), getRefreshToken());
        } catch (IOException e) {
          return null;
        }
      }
    );
  }

  @NotNull
  private static ProgressIndicator getProgressIndicator() {
    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    if (indicator == null) indicator = new EmptyProgressIndicator();
    return indicator;
  }

  @NotNull
  private GiteeServerPath getServer() {
    return GiteeServerPath.Companion.from(getUrl());
  }

  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) return false;
    if (!(o instanceof GiteeRepository)) return false;

    GiteeRepository that = (GiteeRepository) o;
    if (!Comparing.equal(getRepoAuthor(), that.getRepoAuthor())) return false;
    if (!Comparing.equal(getRepoName(), that.getRepoName())) return false;
    if (!Comparing.equal(getAccessToken(), that.getAccessToken())) return false;
    if (!Comparing.equal(getRefreshToken(), that.getRefreshToken())) return false;
    if (!Comparing.equal(isAssignedIssuesOnly(), that.isAssignedIssuesOnly())) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return StringUtil.stringHashCode(getRepoName()) + 31 * StringUtil.stringHashCode(getRepoAuthor());
  }

  @Override
  protected int getFeatures() {
    return super.getFeatures() | STATE_UPDATING;
  }
}
