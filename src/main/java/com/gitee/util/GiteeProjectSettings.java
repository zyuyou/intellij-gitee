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
package com.gitee.util;

import com.gitee.api.GiteeRepositoryPath;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/util/GithubProjectSettings.java
 * @author JetBrains s.r.o.
 * @author Aleksey Pivovarov
 */
@State(name = "GiteeProjectSettings", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public class GiteeProjectSettings implements PersistentStateComponent<GiteeProjectSettings.State> {
  private State myState = new State();

  @Override
  public State getState() {
    return myState;
  }

  @Override
  public void loadState(@NotNull State state) {
    myState = state;
  }

  public static GiteeProjectSettings getInstance(@NotNull Project project) {
    return ServiceManager.getService(project, GiteeProjectSettings.class);
  }

  public static class State {
    @Nullable public String CREATE_PULL_REQUEST_DEFAULT_BRANCH = null;
    @Nullable public String CREATE_PULL_REQUEST_DEFAULT_REPO_USER = null;
    @Nullable public String CREATE_PULL_REQUEST_DEFAULT_REPO_NAME = null;
  }

  @Nullable
  public String getCreatePullRequestDefaultBranch() {
    return myState.CREATE_PULL_REQUEST_DEFAULT_BRANCH;
  }

  public void setCreatePullRequestDefaultBranch(@NotNull String branch) {
    myState.CREATE_PULL_REQUEST_DEFAULT_BRANCH = branch;
  }

  @Nullable
  public GiteeRepositoryPath getCreatePullRequestDefaultRepo() {
    if (myState.CREATE_PULL_REQUEST_DEFAULT_REPO_USER == null || myState.CREATE_PULL_REQUEST_DEFAULT_REPO_NAME == null) {
      return null;
    }
    return new GiteeRepositoryPath(myState.CREATE_PULL_REQUEST_DEFAULT_REPO_USER, myState.CREATE_PULL_REQUEST_DEFAULT_REPO_NAME);
  }

  public void setCreatePullRequestDefaultRepo(@NotNull GiteeRepositoryPath repo) {
    myState.CREATE_PULL_REQUEST_DEFAULT_REPO_USER = repo.getOwner();
    myState.CREATE_PULL_REQUEST_DEFAULT_REPO_NAME = repo.getRepository();
  }
}
