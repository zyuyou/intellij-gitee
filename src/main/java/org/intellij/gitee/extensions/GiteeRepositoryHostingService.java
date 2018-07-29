/*
 * Copyright 2016-2018 码云
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
 */

package org.intellij.gitee.extensions;

import com.intellij.dvcs.hosting.RepositoryListLoader;
import com.intellij.dvcs.hosting.RepositoryListLoadingException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import git4idea.DialogManager;
import git4idea.remote.GitRepositoryHostingService;
import org.intellij.gitee.api.GiteeApiUtil;
import org.intellij.gitee.api.data.GiteeRepo;
import org.intellij.gitee.ui.GiteeLoginDialog;
import org.intellij.gitee.util.*;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/extensions/GithubRepositoryHostingService.java
 * @author JetBrains s.r.o.
 */
public class GiteeRepositoryHostingService extends GitRepositoryHostingService {
	@NotNull
	@Override
	public String getServiceDisplayName() {
		return "Gitee";
	}

	@Override
	@NotNull
	public RepositoryListLoader getRepositoryListLoader(@NotNull Project project) {
		return new RepositoryListLoader() {
			@NotNull private final GiteeAuthDataHolder myAuthDataHolder = GiteeAuthDataHolder.createFromSettings();

			@Override
			public boolean isEnabled() {
				return AuthLevel.LOGGED.accepts(myAuthDataHolder.getAuthData());
			}

			@Override
			public boolean enable() {
				GiteeAuthData currentAuthData = myAuthDataHolder.getAuthData();
				myAuthDataHolder.runTransaction(currentAuthData, () -> {
					GiteeLoginDialog dialog = new GiteeLoginDialog(project, currentAuthData, AuthLevel.LOGGED);
					DialogManager.show(dialog);
					if (dialog.isOK()) {
						GiteeAuthData authData = dialog.getAuthData();
						GiteeSettings.getInstance().setAuthData(authData, dialog.isSavePasswordSelected());
						return authData;
					}
					return currentAuthData;
				});
				return isEnabled();
			}

			@NotNull
			@Override
			public List<String> getAvailableRepositories(@NotNull ProgressIndicator progressIndicator) throws RepositoryListLoadingException {
				try {
					return GiteeUtil.runTask(project, myAuthDataHolder, progressIndicator, GiteeApiUtil::getAvailableRepos)
						.stream()
						.sorted(Comparator.comparing(GiteeRepo::getUserName).thenComparing(GiteeRepo::getName))
						.map(repo -> GiteeUrlUtil.getCloneUrl(repo.getFullPath()))
						.collect(Collectors.toList());
				}
				catch (Exception e) {
					throw new RepositoryListLoadingException("Error connecting to Gitee", e);
				}
			}
		};
	}
}
