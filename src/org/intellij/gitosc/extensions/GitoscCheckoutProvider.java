/*
 * Copyright 2013-2016 Yuyou Chow
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
package org.intellij.gitosc.extensions;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.CheckoutProvider;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.actions.BasicAction;
import git4idea.checkout.GitCheckoutProvider;
import git4idea.checkout.GitCloneDialog;
import git4idea.commands.Git;
import org.intellij.gitosc.GitoscConstants;
import org.intellij.gitosc.api.GitoscApiUtil;
import org.intellij.gitosc.api.GitoscRepo;
import org.intellij.gitosc.util.GitoscAuthDataHolder;
import org.intellij.gitosc.util.GitoscNotifications;
import org.intellij.gitosc.util.GitoscUrlUtil;
import org.intellij.gitosc.util.GitoscUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/extensions/GithubCheckoutProvider.java
 */
public class GitoscCheckoutProvider implements CheckoutProvider {
	@Override
	public void doCheckout(@NotNull final Project project, @Nullable Listener listener) {
		if(!GitoscUtil.testGitExecutable(project)){
			return;
		}
		BasicAction.saveAll();

		List<GitoscRepo> availableRepos;

		try{
			availableRepos = GitoscUtil.computeValueInModalIO(project, GitoscConstants.TITLE_ACCESS_TO_GITOSC, indicator ->
				GitoscUtil.runTask(project, GitoscAuthDataHolder.createFromSettings(), indicator, GitoscApiUtil::getAvailableRepos));
		}catch (IOException e){
			GitoscNotifications.showError(project, "Couldn't get the list of GitOSC repositories", e);
			return;
		}

		Collections.sort(availableRepos, (r1, r2) -> {
			final int comparedOwners = r1.getUserName().compareTo(r2.getUserName());
			return comparedOwners != 0 ? comparedOwners : r1.getName().compareTo(r2.getName());
		});

		final GitCloneDialog dialog = new GitCloneDialog(project);
		// Add predefined repositories to history
		dialog.prependToHistory("-----------------------------------------------");
		for (int i = availableRepos.size() - 1; i >= 0; i--) {
			dialog.prependToHistory(GitoscUrlUtil.getCloneUrl(availableRepos.get(i).getFullPath()));
		}

		if(!dialog.showAndGet()){
			return;
		}
		dialog.rememberSettings();

		final VirtualFile destinationParent = LocalFileSystem.getInstance().findFileByIoFile(new File(dialog.getParentDirectory()));
		if(destinationParent == null){
			return;
		}

		final String sourceRepositoryUrl = dialog.getSourceRepositoryURL();
		final String directoryName = dialog.getDirectoryName();
		final String parentDirectory = dialog.getParentDirectory();

		Git git = ServiceManager.getService(Git.class);
		GitCheckoutProvider.clone(project, git, listener, destinationParent, sourceRepositoryUrl, directoryName, parentDirectory);
	}

	@Override
	public String getVcsName() {
		return "Git_OSC";
	}
}
