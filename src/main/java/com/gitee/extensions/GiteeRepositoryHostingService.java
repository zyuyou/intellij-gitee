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
package com.gitee.extensions;

import com.gitee.api.GiteeApiRequestExecutorManager;
import com.gitee.authentication.GiteeAuthenticationManager;
import com.gitee.util.GiteeGitHelper;
import com.gitee.util.GiteeUtil;
import com.intellij.dvcs.hosting.RepositoryListLoader;
import com.intellij.openapi.project.Project;
import git4idea.remote.GitRepositoryHostingService;
import git4idea.remote.InteractiveGitHttpAuthDataProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GiteeRepositoryHostingService extends GitRepositoryHostingService {
  @NotNull
  private final GiteeAuthenticationManager myAuthenticationManager;
  @NotNull
  private final GiteeApiRequestExecutorManager myExecutorManager;
  @NotNull
  private final GiteeGitHelper myGitHelper;
  @NotNull
  private final GiteeHttpAuthDataProvider myAuthDataProvider;

  public GiteeRepositoryHostingService() {
    myAuthenticationManager = GiteeAuthenticationManager.getInstance();
    myExecutorManager = GiteeApiRequestExecutorManager.getInstance();
    myGitHelper = GiteeGitHelper.getInstance();
    myAuthDataProvider = GiteeHttpAuthDataProvider.EP_NAME.findExtensionOrFail(GiteeHttpAuthDataProvider.class);
  }

  @NotNull
  @Override
  public String getServiceDisplayName() {
    return GiteeUtil.SERVICE_DISPLAY_NAME;
  }

  @Override
  @NotNull
  public RepositoryListLoader getRepositoryListLoader(@NotNull Project project) {
    return new RepositoryListLoader() {
      @Override
      public boolean isEnabled() {
        return false;
      }
    };
//    return new RepositoryListLoader() {
//      @NotNull
//      private final Map<GiteeAccount, GiteeApiRequestExecutor> myExecutors = new HashMap<>();
//
//      @Override
//      public boolean isEnabled() {
//        for (GiteeAccount account : myAuthenticationManager.getAccounts()) {
//          try {
//            myExecutors.put(account, myExecutorManager.getExecutor(account));
//          } catch (GiteeMissingTokenException e) {
//            // skip
//          }
//        }
//        return !myExecutors.isEmpty();
//      }
//
//      @Override
//      public boolean enable(@Nullable Component parentComponent) {
//        if (!myAuthenticationManager.ensureHasAccounts(project, parentComponent)) return false;
//
//        boolean atLeastOneHasToken = false;
//
//        for (GiteeAccount account : myAuthenticationManager.getAccounts()) {
//          GiteeApiRequestExecutor executor = myExecutorManager.getExecutor(account, project);
//
//          if (executor == null) continue;
//
//          myExecutors.put(account, executor);
//
//          atLeastOneHasToken = true;
//        }
//
//        return atLeastOneHasToken;
//      }
//
//      @NotNull
//      @Override
//      public Result getAvailableRepositoriesFromMultipleSources(@NotNull ProgressIndicator progressIndicator) {
//        List<String> urls = new ArrayList<>();
//        List<RepositoryListLoadingException> exceptions = new ArrayList<>();
//
//        for (Map.Entry<GiteeAccount, GiteeApiRequestExecutor> entry : myExecutors.entrySet()) {
//          GiteeServerPath server = entry.getKey().getServer();
//          GiteeApiRequestExecutor executor = entry.getValue();
//
//          try {
//            Stream<GiteeRepo> streamAssociated = GiteeApiPagesLoader.loadAll(executor, progressIndicator, GiteeApiRequests.CurrentUser.Repos.pages(server)).stream();
//
//            Stream<GiteeRepo> streamWatched = StreamEx.empty();
//
//            try {
//              streamWatched = GiteeApiPagesLoader.loadAll(executor, progressIndicator, GiteeApiRequests.CurrentUser.RepoSubs.pages(server)).stream();
//            } catch (GiteeAuthenticationException | GiteeStatusCodeException ignore) {
//              // We already can return something useful from getUserRepos, so let's ignore errors.
//              // One of this may not exist in GitHub enterprise
//            }
//
//            urls.addAll(Stream.concat(streamAssociated, streamWatched)
//                .sorted(Comparator.comparing(GiteeRepo::getUserName).thenComparing(GiteeRepo::getName))
//                .map(repo -> myGitHelper.getRemoteUrl(server, repo.getFullName()))
//                .collect(Collectors.toList()));
//
//          } catch (Exception e) {
//            exceptions.add(new RepositoryListLoadingException("Cannot load repositories from Gitee", e));
//          }
//        }
//        return new Result(urls, exceptions);
//      }
//    };
  }

  @Nullable
  @Override
  public InteractiveGitHttpAuthDataProvider getInteractiveAuthDataProvider(@NotNull Project project, @NotNull String url) {
    return getProvider(project, url, null);
  }

  @Nullable
  @Override
  public InteractiveGitHttpAuthDataProvider getInteractiveAuthDataProvider(@NotNull Project project, @NotNull String url, @NotNull String login) {
    return getProvider(project, url, login);
  }

  @Nullable
  private InteractiveGitHttpAuthDataProvider getProvider(@NotNull Project project, @NotNull String url, @Nullable String login) {
//    Set<GiteeAccount> potentialAccounts = myAuthDataProvider.getSuitableAccounts(project, url, login);
//
////    if (potentialAccounts.isEmpty()) return null;
////
////    return new InteractiveGiteeHttpAuthDataProvider(project, potentialAccounts, myAuthenticationManager);
//
//    if (!potentialAccounts.isEmpty()) {
//      return new InteractiveSelectGiteeAccountHttpAuthDataProvider(project, potentialAccounts, myAuthenticationManager);
//    }
//
//    if (GiteeServerPath.Companion.getDEFAULT_SERVER().matches(url)) {
//      return new InteractiveCreateGiteeAccountHttpAuthDataProvider(project, myAuthenticationManager,
//          GiteeServerPath.Companion.getDEFAULT_SERVER(), login);
//    }

    return null;
  }
}
