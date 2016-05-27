package org.intellij.gitosc.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import javax.swing.*;
import java.util.Set;
import java.util.regex.Pattern;

public class GitoscShareDialog extends DialogWrapper {
	private static final Pattern GITOSC_REPO_PATTERN = Pattern.compile("[a-zA-Z0-9_.-]+");
	private final GitoscSharePanel myGitoscSharePanel;
	private final Set<String> myAvailableNames;
	private final Set<String> myAvailableRemotes;

	public GitoscShareDialog(Project project, Set<String> availableNames, Set<String> availableRemotes, boolean privateRepoAllowed) {
		super(project);
		myAvailableNames = availableNames;
		myAvailableRemotes = availableRemotes;
		myGitoscSharePanel = new GitoscSharePanel(this);
		init();
		setTitle("Share Project On GitOSC");
		setOKButtonText("Share");
		myGitoscSharePanel.setRepositoryName(project.getName());
		myGitoscSharePanel.setRemoteName(availableRemotes.isEmpty() ? "origin" : "gitosc");
		myGitoscSharePanel.setPrivateRepoAvailable(privateRepoAllowed);
		init();
		updateOkButton();
	}

	@Override
	protected String getHelpId() {
		return "gitosc.share";
	}

	@Override
	protected String getDimensionServiceKey() {
		return "GitOSC.ShareDialog";
	}

	@Override
	protected JComponent createCenterPanel() {
		return myGitoscSharePanel.getPanel();
	}

	@Override
	public JComponent getPreferredFocusedComponent() {
		return myGitoscSharePanel.getPreferredFocusComponent();
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
		if (!GITOSC_REPO_PATTERN.matcher(repositoryName).matches()) {
			setErrorText("Invalid repository name. Name should consist of letters, numbers, dashes, dots and underscores");
			setOKActionEnabled(false);
			return;
		}
		setErrorText(null);
		setOKActionEnabled(true);
	}

	public String getRepositoryName() {
		return myGitoscSharePanel.getRepositoryName();
	}

	public boolean isPrivate() {
		return myGitoscSharePanel.isPrivate();
	}

	public String getDescription() {
		return myGitoscSharePanel.getDescription();
	}

	public String getRemoteName() {
		return myGitoscSharePanel.getRemoteName();
	}

	@TestOnly
	public void testSetRepositoryName(@NotNull String name) {
		myGitoscSharePanel.setRepositoryName(name);
	}
}
