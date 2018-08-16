// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.extensions;

import com.gitee.api.GiteeApiRequestExecutor;
import com.gitee.api.GiteeApiRequestExecutorManager;
import com.gitee.api.GiteeApiRequests;
import com.gitee.api.GiteeServerPath;
import com.gitee.api.data.GiteeRepo;
import com.gitee.api.util.GiteeApiPagesLoader;
import com.gitee.authentication.GiteeAuthenticationManager;
import com.gitee.authentication.accounts.GiteeAccount;
import com.gitee.exceptions.GiteeAuthenticationException;
import com.gitee.exceptions.GiteeMissingTokenException;
import com.gitee.exceptions.GiteeStatusCodeException;
import com.gitee.util.GiteeGitHelper;
import com.gitee.util.GiteeUtil;
import com.intellij.dvcs.hosting.RepositoryListLoader;
import com.intellij.dvcs.hosting.RepositoryListLoadingException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import git4idea.remote.GitRepositoryHostingService;
import git4idea.remote.InteractiveGitHttpAuthDataProvider;
import one.util.streamex.StreamEx;
import org.jetbrains.annotations.CalledInBackground;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GiteeRepositoryHostingService extends GitRepositoryHostingService {
	@NotNull
	private final GiteeAuthenticationManager myAuthenticationManager;
	@NotNull
	private final GiteeApiRequestExecutorManager myExecutorManager;
	@NotNull
	private final GiteeGitHelper myGitHelper;
	@NotNull
	private final GiteeHttpAuthDataProvider myAuthDataProvider;

	public GiteeRepositoryHostingService(@NotNull GiteeAuthenticationManager manager, @NotNull GiteeApiRequestExecutorManager executorManager, @NotNull GiteeGitHelper gitHelper, @NotNull GiteeHttpAuthDataProvider authDataProvider) {
		myAuthenticationManager = manager;
		myExecutorManager = executorManager;
		myGitHelper = gitHelper;
		myAuthDataProvider = authDataProvider;
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
			@NotNull
			private final Map<GiteeAccount, GiteeApiRequestExecutor> myExecutors = new HashMap<>();

			@Override
			public boolean isEnabled() {
				for (GiteeAccount account : myAuthenticationManager.getAccounts()) {
					try {
						myExecutors.put(account, myExecutorManager.getExecutor(account));
					} catch (GiteeMissingTokenException e) {
						// skip
					}
				}
				return !myExecutors.isEmpty();
			}

			@Override
			public boolean enable(@Nullable Component parentComponent) {
				if (!myAuthenticationManager.ensureHasAccounts(project, parentComponent)) return false;

				boolean atLeastOneHasToken = false;

				for (GiteeAccount account : myAuthenticationManager.getAccounts()) {
					GiteeApiRequestExecutor executor = myExecutorManager.getExecutor(account, project);

					if (executor == null) continue;

					myExecutors.put(account, executor);

					atLeastOneHasToken = true;
				}

				return atLeastOneHasToken;
			}

			@NotNull
			@Override
			public Result getAvailableRepositoriesFromMultipleSources(@NotNull ProgressIndicator progressIndicator) {
				List<String> urls = new ArrayList<>();
				List<RepositoryListLoadingException> exceptions = new ArrayList<>();

				for (Map.Entry<GiteeAccount, GiteeApiRequestExecutor> entry : myExecutors.entrySet()) {
					GiteeServerPath server = entry.getKey().getServer();
					GiteeApiRequestExecutor executor = entry.getValue();

					try {
						Stream<GiteeRepo> streamAssociated = GiteeApiPagesLoader.loadAll(executor, progressIndicator, GiteeApiRequests.CurrentUser.Repos.pages(server)).stream();

						Stream<GiteeRepo> streamWatched = StreamEx.empty();

						try {
							streamWatched = GiteeApiPagesLoader.loadAll(executor, progressIndicator, GiteeApiRequests.CurrentUser.RepoSubs.pages(server)).stream();
						} catch (GiteeAuthenticationException | GiteeStatusCodeException ignore) {
							// We already can return something useful from getUserRepos, so let's ignore errors.
							// One of this may not exist in GitHub enterprise
						}

						urls.addAll(Stream.concat(streamAssociated, streamWatched)
							.sorted(Comparator.comparing(GiteeRepo::getUserName).thenComparing(GiteeRepo::getName))
							.map(repo -> myGitHelper.getRemoteUrl(server, repo.getFullName()))
							.collect(Collectors.toList()));

					} catch (Exception e) {
						exceptions.add(new RepositoryListLoadingException("Cannot load repositories from Gitee", e));
					}
				}
				return new Result(urls, exceptions);
			}
		};
	}

	@CalledInBackground
	@Nullable
	@Override
	public InteractiveGitHttpAuthDataProvider getInteractiveAuthDataProvider(@NotNull Project project, @NotNull String url) {
		return getProvider(project, url, null);
	}

	@CalledInBackground
	@Nullable
	@Override
	public InteractiveGitHttpAuthDataProvider getInteractiveAuthDataProvider(@NotNull Project project, @NotNull String url, @NotNull String login) {
		return getProvider(project, url, login);
	}

	@Nullable
	private InteractiveGitHttpAuthDataProvider getProvider(@NotNull Project project, @NotNull String url, @Nullable String login) {
		Set<GiteeAccount> potentialAccounts = myAuthDataProvider.getSuitableAccounts(project, url, login);

		if (potentialAccounts.isEmpty()) return null;

		return new InteractiveGiteeHttpAuthDataProvider(project, potentialAccounts, myAuthenticationManager);
	}
}
