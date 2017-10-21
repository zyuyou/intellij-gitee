/*
 * Copyright 2016-2017 码云
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.intellij.gitosc.tasks;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.PasswordUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.tasks.*;
import com.intellij.tasks.impl.BaseRepository;
import com.intellij.tasks.impl.BaseRepositoryImpl;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.xmlb.annotations.Tag;
import com.intellij.util.xmlb.annotations.Transient;
import icons.TasksCoreIcons;
import org.intellij.gitosc.GitoscConstants;
import org.intellij.gitosc.api.GitoscApiUtil;
import org.intellij.gitosc.api.GitoscConnection;
import org.intellij.gitosc.api.data.GitoscIssue;
import org.intellij.gitosc.api.data.GitoscIssueComment;
import org.intellij.gitosc.exceptions.*;
import org.intellij.gitosc.icons.GitoscIcons;
import org.intellij.gitosc.util.GitoscAuthData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/tasks/GithubRepository.java
 * @author JetBrains s.r.o.
 * @author Dennis.Ushakov
 */
@Tag("GitOSC")
public class GitoscRepository extends BaseRepositoryImpl {
  private static final Logger LOG = GitoscConstants.LOG;

  private Pattern myPattern = Pattern.compile("($^)");
  @NotNull
  private String myRepoAuthor = "";
  @NotNull
  private String myRepoName = "";
  @NotNull
  private String myUser = "";
  @NotNull
  private String myToken = "";
  private boolean myAssignedIssuesOnly = false;

  @SuppressWarnings({"UnusedDeclaration"})
  public GitoscRepository() {
  }

  public GitoscRepository(GitoscRepository other) {
    super(other);
    setRepoName(other.myRepoName);
    setRepoAuthor(other.myRepoAuthor);
    setToken(other.myToken);
    setAssignedIssuesOnly(other.myAssignedIssuesOnly);
  }

  public GitoscRepository(GitoscRepositoryType type) {
    super(type);
    setUrl("https://" + GitoscConstants.DEFAULT_GITOSC_HOST);
  }

  @NotNull
  @Override
  public CancellableConnection createCancellableConnection() {
    return new CancellableConnection() {
      private final GitoscConnection myConnection = new GitoscConnection(getAuthData(), false);

      @Override
      protected void doTest() throws Exception {
        try {
          GitoscApiUtil.getIssuesQueried(myConnection, getRepoAuthor(), getRepoName(), null, null, false);
        }
        catch (GitoscOperationCanceledException ignore) {
          ignore.printStackTrace();
        }
      }

      @Override
      public void cancel() {
        myConnection.abort();
      }
    };
  }

  @Override
  public boolean isConfigured() {
    return super.isConfigured() &&
           !StringUtil.isEmptyOrSpaces(getRepoAuthor()) &&
           !StringUtil.isEmptyOrSpaces(getRepoName()) &&
           !StringUtil.isEmptyOrSpaces(getToken());
  }

  @Override
  public String getPresentableName() {
    final String name = super.getPresentableName();
    return name +
           (!StringUtil.isEmpty(getRepoAuthor()) ? "/" + getRepoAuthor() : "") +
           (!StringUtil.isEmpty(getRepoName()) ? "/" + getRepoName() : "");
  }

  @Override
  public Task[] getIssues(@Nullable String query, int offset, int limit, boolean withClosed) throws Exception {
    try {
      return getIssues(query, offset + limit, withClosed);
    }
    catch (GitoscRateLimitExceededException e) {
      return new Task[0];
    }
    catch (GitoscAuthenticationException | GitoscStatusCodeException e) {
      throw new Exception(e.getMessage(), e); // Wrap to show error message
    }
    catch (GitoscJsonException e) {
      throw new Exception("Bad response format", e);
    }
  }

  @Override
  public Task[] getIssues(@Nullable String query, int offset, int limit, boolean withClosed, @NotNull ProgressIndicator cancelled)
    throws Exception {
    return getIssues(query, offset, limit, withClosed);
  }

  @NotNull
  private Task[] getIssues(@Nullable String query, int max, boolean withClosed) throws Exception {
    GitoscConnection connection = getConnection();

    try {
      String assigned = null;
      if (myAssignedIssuesOnly) {
        if (StringUtil.isEmptyOrSpaces(myUser)) {
          myUser = GitoscApiUtil.getCurrentUser(connection).getLogin();
        }
        assigned = myUser;
      }

      List<GitoscIssue> issues;
      if (StringUtil.isEmptyOrSpaces(query)) {
        // search queries have way smaller request number limit
        issues = GitoscApiUtil.getIssuesAssigned(connection, getRepoAuthor(), getRepoName(), assigned, max, withClosed);
      }
      else {
        issues = GitoscApiUtil.getIssuesQueried(connection, getRepoAuthor(), getRepoName(), assigned, query, withClosed);
      }

      return ContainerUtil.map2Array(issues, Task.class, this::createTask);
    }
    finally {
      connection.close();
    }
  }

  @NotNull
  private Task createTask(final GitoscIssue issue) {
    return new Task() {
      @NotNull
      String myRepoName = getRepoName();

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

      public String getDescription() {
        return issue.getBody();
      }

      @NotNull
      @Override
      public Comment[] getComments() {
        try {
          return fetchComments(issue.getNumber());
        }
        catch (Exception e) {
          LOG.warn("Error fetching comments for " + issue.getNumber(), e);
          return Comment.EMPTY_ARRAY;
        }
      }

      @NotNull
      @Override
      public Icon getIcon() {
        return GitoscIcons.GITOSC_SMALL;
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
        return !"open".equals(issue.getState());
      }

      @Override
      public TaskRepository getRepository() {
        return GitoscRepository.this;
      }

      @Override
      public String getPresentableName() {
        return getId() + ": " + getSummary();
      }
    };
  }

  private Comment[] fetchComments(final String id) throws Exception {
    GitoscConnection connection = getConnection();
    try {
      List<GitoscIssueComment> result = GitoscApiUtil.getIssueComments(connection, getRepoAuthor(), getRepoName(), id);

      return ContainerUtil.map2Array(result, Comment.class, comment -> new GitoscComment(comment.getCreatedAt(),
                                                                                         comment.getUser().getLogin(),
                                                                                         comment.getBodyHtml(),
                                                                                         comment.getUser().getAvatarUrl(),
                                                                                         comment.getUser().getHtmlUrl()));
    }
    finally {
      connection.close();
    }
  }

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
    GitoscConnection connection = getConnection();
    try {
      return createTask(GitoscApiUtil.getIssue(connection, getRepoAuthor(), getRepoName(), numericId));
    }
    catch (GitoscStatusCodeException e) {
      if (e.getStatusCode() == 404) {
        return null;
      }
      throw e;
    }
    finally {
      connection.close();
    }
  }

  @Override
  public void setTaskState(@NotNull Task task, @NotNull TaskState state) throws Exception {
    GitoscConnection connection = getConnection();
    try {
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
      GitoscApiUtil.setIssueState(connection, getRepoAuthor(), getRepoName(), task.getNumber(), task.getSummary(), isOpen);
    }
    finally {
      connection.close();
    }
  }

  @NotNull
  @Override
  public BaseRepository clone() {
    return new GitoscRepository(this);
  }

  @NotNull
  public String getRepoName() {
    return myRepoName;
  }

  public void setRepoName(@NotNull String repoName) {
    myRepoName = repoName;
    myPattern = Pattern.compile("(" + StringUtil.escapeToRegexp(repoName) + "\\-\\d+)");
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
  public String getToken() {
    return myToken;
  }

  public void setToken(@NotNull String token) {
    myToken = token;
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
    return PasswordUtil.encodePassword(getToken());
  }

  public void setEncodedToken(String password) {
    try {
      setToken(PasswordUtil.decodePassword(password));
    }
    catch (NumberFormatException e) {
      LOG.warn("Can't decode token", e);
    }
  }

  private GitoscAuthData getAuthData() {
    return GitoscAuthData.createTokenAuth(getUrl(), getToken(), isUseProxy());
  }

  private GitoscConnection getConnection() {
    return new GitoscConnection(getAuthData(), true);
  }

  @Override
  public boolean equals(Object o) {
    if (!super.equals(o)) return false;
    if (!(o instanceof GitoscRepository)) return false;

    GitoscRepository that = (GitoscRepository)o;
    if (!Comparing.equal(getRepoAuthor(), that.getRepoAuthor())) return false;
    if (!Comparing.equal(getRepoName(), that.getRepoName())) return false;
    if (!Comparing.equal(getToken(), that.getToken())) return false;
    if (!Comparing.equal(isAssignedIssuesOnly(), that.isAssignedIssuesOnly())) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return StringUtil.stringHashCode(getRepoName()) +
           31 * StringUtil.stringHashCode(getRepoAuthor());
  }

  @Override
  protected int getFeatures() {
    return super.getFeatures() | STATE_UPDATING;
  }
}
