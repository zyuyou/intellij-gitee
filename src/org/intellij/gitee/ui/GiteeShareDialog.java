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
package org.intellij.gitee.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;
import org.intellij.gitee.GiteeBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/ui/GithubShareDialog.java
 * @author JetBrains s.r.o.
 * @author oleg
 * @date 10/22/10
 */
public class GiteeShareDialog extends DialogWrapper {
	private static final Pattern GITEE_REPO_PATTERN = Pattern.compile("[a-zA-Z0-9_.-]+");
	private final GiteeSharePanel myGiteeSharePanel;
	private final Set<String> myAvailableNames;
	private final Set<String> myAvailableRemotes;

	public GiteeShareDialog(Project project, Set<String> availableNames, Set<String> availableRemotes, boolean privateRepoAllowed) {
		super(project);
		myAvailableNames = availableNames;
		myAvailableRemotes = availableRemotes;
		myGiteeSharePanel = new GiteeSharePanel(this);
		init();
		setTitle(GiteeBundle.message2("gitee.share.project.title"));
		setOKButtonText(GiteeBundle.message2("gitee.dialog.share.button.text"));
		setCancelButtonText(GiteeBundle.message2("gitee.dialog.cancel.button.text"));
		getHelpAction().putValue(Action.NAME, GiteeBundle.message2("gitee.dialog.help.button.text"));
		myGiteeSharePanel.setRepositoryName(project.getName());
		myGiteeSharePanel.setRemoteName(availableRemotes.isEmpty() ? "origin" : "gitee");
		myGiteeSharePanel.setPrivateRepoAvailable(privateRepoAllowed);
		init();
		updateOkButton();
	}

	@Override
	protected String getHelpId() {
		return "gitee.share";
	}

	@Override
	protected String getDimensionServiceKey() {
		return "GitOSC.ShareDialog";
	}

	@Override
	protected JComponent createCenterPanel() {
		return myGiteeSharePanel.getPanel();
	}

	@Override
	public JComponent getPreferredFocusedComponent() {
		return myGiteeSharePanel.getPreferredFocusComponent();
	}

	public void updateOkButton() {
		String repositoryName = getRepositoryName();
		String remoteName = getRemoteName();
		if (StringUtil.isEmpty(repositoryName)) {
			setErrorText("No repository name selected");
			setOKActionEnabled(false);
			return;
		}
		if (myAvailableNames.contains(repositoryName)) {
			setErrorText("Repository with selected name already exists");
			setOKActionEnabled(false);
			return;
		}
		if (myAvailableRemotes.contains(remoteName)) {
			setErrorText("Remote with selected name already exists");
			setOKActionEnabled(false);
			return;
		}
		if (!GITEE_REPO_PATTERN.matcher(repositoryName).matches()) {
			setErrorText("Invalid repository name. Name should consist of letters, numbers, dashes, dots and underscores");
			setOKActionEnabled(false);
			return;
		}
		setErrorText(null);
		setOKActionEnabled(true);
	}

	public String getRepositoryName() {
		return myGiteeSharePanel.getRepositoryName();
	}

	public boolean isPrivate() {
		return myGiteeSharePanel.isPrivate();
	}

	public String getDescription() {
		return myGiteeSharePanel.getDescription();
	}

	public String getRemoteName() {
		return myGiteeSharePanel.getRemoteName();
	}

	@TestOnly
	public void testSetRepositoryName(@NotNull String name) {
		myGiteeSharePanel.setRepositoryName(name);
	}
}
