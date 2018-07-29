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
package org.intellij.gitee.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.changes.ui.SelectFilesDialog;
import com.intellij.openapi.vcs.ui.CommitMessage;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.HashSet;
import com.intellij.vcsUtil.VcsFileUtil;
import git4idea.DialogManager;
import git4idea.GitLocalBranch;
import git4idea.GitUtil;
import git4idea.actions.BasicAction;
import git4idea.actions.GitInit;
import git4idea.commands.*;
import git4idea.i18n.GitBundle;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import git4idea.util.GitFileUtils;
import git4idea.util.GitUIUtil;
import org.apache.http.HttpStatus;
import org.intellij.gitee.GiteeConstants;
import org.intellij.gitee.GiteeBundle;
import org.intellij.gitee.api.GiteeApiUtil;
import org.intellij.gitee.api.GiteeFullPath;
import org.intellij.gitee.api.data.GiteeRepo;
import org.intellij.gitee.api.data.GiteeUserDetailed;
import org.intellij.gitee.exceptions.GiteeStatusCodeException;
import org.intellij.gitee.icons.GiteeIcons;
import org.intellij.gitee.ui.GiteeShareDialog;
import org.intellij.gitee.util.GiteeUrlUtil;
import org.intellij.gitee.util.GiteeUtil;
import org.intellij.gitee.util.GiteeAuthDataHolder;
import org.intellij.gitee.util.GiteeNotifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.*;

import static org.intellij.gitee.GiteeConstants.LOG;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/GithubShareAction.java
 * @author JetBrains s.r.o.
 * @author oleg
 */
public class GiteeShareAction extends DumbAwareAction {
	public GiteeShareAction() {
		super(GiteeBundle.message2("gitee.share.project.title"), GiteeBundle.message2("gitee.share.project.desc"), GiteeIcons.GITEE_SMALL);
	}

	@Override
	public void actionPerformed(AnActionEvent e) {
		final Project project = e.getData(CommonDataKeys.PROJECT);
		final VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);

		if(project == null || project.isDisposed() || !GiteeUtil.testGitExecutable(project)){
			return;
		}

		shareProjectOnGitee(project, file);
	}

	public static void shareProjectOnGitee(@NotNull final Project project, @Nullable final VirtualFile file){
		BasicAction.saveAll();

		// get gitRepository
		final GitRepository gitRepository = GiteeUtil.getGitRepository(project, file);
		final boolean gitDetected = gitRepository != null;
		final VirtualFile root = gitDetected ? gitRepository.getRoot() : project.getBaseDir();

		final GiteeAuthDataHolder authDataHolder = GiteeAuthDataHolder.createFromSettings();

		// check for existing git repo
		Set<String> existingRemotes = Collections.emptySet();
		if(gitDetected){
			final String giteeRemote = GiteeUtil.findGiteeRemoteUrl(gitRepository);
			if(giteeRemote != null){
				if(!checkExistingRemote(project, authDataHolder, giteeRemote)) return;
			}

			existingRemotes = ContainerUtil.map2Set(gitRepository.getRemotes(), GitRemote::getName);
		}

		// get available Gitee repos with modal progress
		final GiteeInfo giteeInfo = loadGiteeInfoWithModal(authDataHolder, project);
		if(giteeInfo == null){
			return;
		}

		// show dialog
		final GiteeShareDialog shareDialog = new GiteeShareDialog(project, giteeInfo.getRepositoryNames(), existingRemotes, true);
		DialogManager.show(shareDialog);
		if (!shareDialog.isOK()) {
			return;
		}
		final boolean isPrivate = shareDialog.isPrivate();
		final String name = shareDialog.getRepositoryName();
		final String description = shareDialog.getDescription();
		final String remoteName = shareDialog.getRemoteName();

		new Task.Backgroundable(project, "Sharing Project on Gitee...") {
			@Override
			public void run(@NotNull ProgressIndicator indicator) {
				// create Gitee repo
				LOG.info("Creating Gitee repository");
				indicator.setText("Creating Gitee repository...");
				final String url = createGiteeRepository(project, authDataHolder, indicator, name, description, isPrivate);
				if (url == null) {
					return;
				}
				LOG.info("Successfully created Gitee repository");

				// creating empty git repo if git is not initialized
				LOG.info("Binding local project with Gitee");
				if (!gitDetected) {
					LOG.info("No git detected, creating empty git repo");
					indicator.setText("Creating empty git repo...");
					if (!createEmptyGitRepository(project, root, indicator)) {
						return;
					}
				}

				GitRepositoryManager repositoryManager = GitUtil.getRepositoryManager(project);
				final GitRepository repository = repositoryManager.getRepositoryForRoot(root);
				if (repository == null) {
					GiteeNotifications.showError(project, "Failed to create Gitee Repository", "Can't find Git repository");
					return;
				}

				final String remoteUrl = GiteeUrlUtil.getCloneUrl(giteeInfo.getUser().getLogin(), name);

				//git remote add origin git@gitee.com:login/name.git
				LOG.info("Adding Gitee as a remote host");
				indicator.setText("Adding Gitee as a remote host...");
				if (!GiteeUtil.addGiteeRemote(project, repository, remoteName, remoteUrl)) {
					return;
				}

				// create sample commit for binding project
				if (!performFirstCommitIfRequired(project, root, repository, indicator, name, url)) {
					return;
				}

				//git push origin master
				LOG.info("Pushing to gitee master");
				indicator.setText("Pushing to gitee master...");
				if (!pushCurrentBranch(project, repository, remoteName, remoteUrl, name, url)) {
					return;
				}

				GiteeNotifications.showInfoURL(project, GiteeBundle.message2("gitee.share.project.success.notify"), name, url);
			}
		}.queue();
	}

	@Nullable
	private static String createGiteeRepository(@NotNull Project project,
	                                            @NotNull GiteeAuthDataHolder authHolder,
	                                            @NotNull ProgressIndicator indicator,
	                                            @NotNull final String name,
	                                            @NotNull final String description,
	                                            final boolean isPrivate) {

		try {
			return GiteeUtil.runTask(project, authHolder, indicator, connection ->
				GiteeApiUtil.createRepo(connection, name, description, isPrivate)).getHtmlUrl();
		}
		catch (IOException e) {
			GiteeNotifications.showError(project, "Failed to create Gitee Repository", e);
			return null;
		}
	}

	private static boolean createEmptyGitRepository(@NotNull Project project,
	                                                @NotNull VirtualFile root,
	                                                @NotNull ProgressIndicator indicator) {
		final GitLineHandler h = new GitLineHandler(project, root, GitCommand.INIT);
		h.setStdoutSuppressed(false);
		GitHandlerUtil.runInCurrentThread(h, indicator, true, GitBundle.getString("initializing.title"));
		if (!h.errors().isEmpty()) {
			GitUIUtil.showOperationErrors(project, h.errors(), "git init");
			LOG.info("Failed to create empty git repo: " + h.errors());
			return false;
		}
		GitInit.refreshAndConfigureVcsMappings(project, root, root.getPath());
		return true;
	}

	private static boolean checkExistingRemote(@NotNull final Project project,
	                                           @NotNull final GiteeAuthDataHolder authHolder,
	                                           @NotNull String remote) {

		final GiteeFullPath path = GiteeUrlUtil.getUserAndRepositoryFromRemoteUrl(remote);
		if(path == null){
			return GiteeNotifications.showYesNoDialog(project,
				GiteeBundle.message2("gitee.project.is.already.on.text"),
				"Can't connect to repository from configured remote. You could want to check .git config.\n" +
					"Do you want to proceed anyway?");
		}

		try{
			GiteeRepo repo = GiteeUtil.computeValueInModalIO(project, GiteeConstants.TITLE_ACCESS_TO_GITEE, indicator ->
				GiteeUtil.runTask(project, authHolder, indicator, connection ->
					GiteeApiUtil.getDetailedRepoInfo(connection, path.getUser(), path.getRepository())));

			int result = Messages.showDialog(project,
				"Successfully connected to " + repo.getHtmlUrl() + ".\n" +
					"Do you want to proceed anyway?",
				GiteeBundle.message2("gitee.project.is.already.on.text"),
				new String[]{"Continue", "Open in Browser", Messages.CANCEL_BUTTON}, 2, Messages.getQuestionIcon());

			if(result == 0) return true;

			if (result == 1) {
				BrowserUtil.browse(repo.getHtmlUrl());
			}
			return false;
		}
		catch (GiteeStatusCodeException e) {
			if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
				return GiteeNotifications.showYesNoDialog(project,
					GiteeBundle.message2("gitee.project.is.already.on.text"),
					"Can't connect to repository from configured remote. You could want to check .git config.\n" +
						"Do you want to proceed anyway?");
			}

			GiteeNotifications.showErrorDialog(project, GiteeBundle.message2("gitee.access.fail.dialog.title"), e);
			return false;
		}
		catch (IOException e) {
			GiteeNotifications.showErrorDialog(project, GiteeBundle.message2("gitee.access.fail.dialog.title"), e);
			return false;
		}
	}


	private static boolean performFirstCommitIfRequired(@NotNull final Project project,
	                                                    @NotNull VirtualFile root,
	                                                    @NotNull GitRepository repository,
	                                                    @NotNull ProgressIndicator indicator,
	                                                    @NotNull String name,
	                                                    @NotNull String url) {
		// check if there is no commits
		if (!repository.isFresh()) {
			return true;
		}

		LOG.info("Trying to commit");
		try {
			LOG.info("Adding files for commit");
			indicator.setText("Adding files to git...");

			// ask for files to add
			final List<VirtualFile> trackedFiles = ChangeListManager.getInstance(project).getAffectedFiles();
			final Collection<VirtualFile> untrackedFiles =
				filterOutIgnored(project, repository.getUntrackedFilesHolder().retrieveUntrackedFiles());
			trackedFiles.removeAll(untrackedFiles); // fix IDEA-119855

			final List<VirtualFile> allFiles = new ArrayList<VirtualFile>();
			allFiles.addAll(trackedFiles);
			allFiles.addAll(untrackedFiles);

			final Ref<GiteeUntrackedFilesDialog> dialogRef = new Ref<GiteeUntrackedFilesDialog>();
			ApplicationManager.getApplication().invokeAndWait(() -> {
				GiteeUntrackedFilesDialog dialog = new GiteeUntrackedFilesDialog(project, allFiles);
				if (!trackedFiles.isEmpty()) {
					dialog.setSelectedFiles(trackedFiles);
				}
				DialogManager.show(dialog);
				dialogRef.set(dialog);
			}, indicator.getModalityState());
			final GiteeUntrackedFilesDialog dialog = dialogRef.get();

			final Collection<VirtualFile> files2commit = dialog.getSelectedFiles();
			if (!dialog.isOK() || files2commit.isEmpty()) {
				GiteeNotifications.showInfoURL(project, GiteeBundle.message2("gitee.create.empty.repository"), name, url);
				return false;
			}

			Collection<VirtualFile> files2add = ContainerUtil.intersection(untrackedFiles, files2commit);
			Collection<VirtualFile> files2rm = ContainerUtil.subtract(trackedFiles, files2commit);
			Collection<VirtualFile> modified = new HashSet<VirtualFile>(trackedFiles);
			modified.addAll(files2commit);

			GitFileUtils.addFiles(project, root, files2add);
			GitFileUtils.deleteFilesFromCache(project, root, files2rm);

			// commit
			LOG.info("Performing commit");
			indicator.setText("Performing commit...");
			GitSimpleHandler handler = new GitSimpleHandler(project, root, GitCommand.COMMIT);
			handler.setStdoutSuppressed(false);
			handler.addParameters("-m", dialog.getCommitMessage());
			handler.endOptions();
			handler.run();

			VcsFileUtil.markFilesDirty(project, modified);
		}
		catch (VcsException e) {
			LOG.warn(e);
			GiteeNotifications.showErrorURL(project, "Can't finish Gitee sharing process", "Successfully created project ", "'" + name + "'",
				" on Gitee, but initial commit failed:<br/>" + GiteeUtil.getErrorTextFromException(e), url);
			return false;
		}
		LOG.info("Successfully created initial commit");
		return true;
	}

	@NotNull
	private static Collection<VirtualFile> filterOutIgnored(@NotNull Project project, @NotNull Collection<VirtualFile> files) {
		final ChangeListManager changeListManager = ChangeListManager.getInstance(project);
		final ProjectLevelVcsManager vcsManager = ProjectLevelVcsManager.getInstance(project);
		return ContainerUtil.filter(files, file -> !changeListManager.isIgnoredFile(file) && !vcsManager.isIgnored(file));
	}

	private static boolean pushCurrentBranch(@NotNull Project project,
	                                         @NotNull GitRepository repository,
	                                         @NotNull String remoteName,
	                                         @NotNull String remoteUrl,
	                                         @NotNull String name,
	                                         @NotNull String url) {
		Git git = ServiceManager.getService(Git.class);

		GitLocalBranch currentBranch = repository.getCurrentBranch();
		if (currentBranch == null) {
			GiteeNotifications.showErrorURL(project, "Can't finish Gitee sharing process", "Successfully created project ", "'" + name + "'",
				" on Gitee, but initial push failed: no current branch", url);
			return false;
		}
		GitCommandResult result = git.push(repository, remoteName, remoteUrl, currentBranch.getName(), true);
		if (!result.success()) {
			GiteeNotifications.showErrorURL(project, "Can't finish Gitee sharing process", "Successfully created project ", "'" + name + "'",
				" on Gitee, but initial push failed:<br/>" + result.getErrorOutputAsHtmlString(), url);
			return false;
		}
		return true;
	}

	@Nullable
	private static GiteeInfo loadGiteeInfoWithModal(@NotNull final GiteeAuthDataHolder authHolder, @NotNull final Project project) {
		try {
			return GiteeUtil.computeValueInModalIO(project, GiteeConstants.TITLE_ACCESS_TO_GITEE, indicator -> {
				// get existing gitee repos (network) and validate auth data
				return GiteeUtil.runTask(project, authHolder, indicator, connection -> {
					// check access to private repos (network)
					GiteeUserDetailed userInfo = GiteeApiUtil.getCurrentUserDetailed(connection);

					HashSet<String> names = new HashSet<String>();
					for (GiteeRepo info : GiteeApiUtil.getUserRepos(connection)) {
						names.add(info.getName());
					}
					return new GiteeInfo(userInfo, names);
				});
			});
		}
		catch (IOException e) {
			GiteeNotifications.showErrorDialog(project, GiteeBundle.message2("gitee.access.fail.dialog.title"), e);
			return null;
		}
	}

	public static class GiteeUntrackedFilesDialog extends SelectFilesDialog implements TypeSafeDataProvider {
		@NotNull private final Project myProject;
		private CommitMessage myCommitMessagePanel;

		public GiteeUntrackedFilesDialog(@NotNull Project project, @NotNull List<VirtualFile> untrackedFiles) {
			super(project, untrackedFiles, null, null, true, false, false);
			myProject = project;
			setTitle("Add Files For Initial Commit");
			init();
		}

		@Override
		protected JComponent createNorthPanel() {
			return null;
		}

		@Override
		protected JComponent createCenterPanel() {
			final JComponent tree = super.createCenterPanel();

			myCommitMessagePanel = new CommitMessage(myProject);
			myCommitMessagePanel.setCommitMessage("Initial commit");

			Splitter splitter = new Splitter(true);
			splitter.setHonorComponentsMinimumSize(true);
			splitter.setFirstComponent(tree);
			splitter.setSecondComponent(myCommitMessagePanel);
			splitter.setProportion(0.7f);

			return splitter;
		}

		@NotNull
		public String getCommitMessage() {
			return myCommitMessagePanel.getComment();
		}

		@Override
		public void calcData(DataKey key, DataSink sink) {
			if (key == VcsDataKeys.COMMIT_MESSAGE_CONTROL) {
				sink.put(VcsDataKeys.COMMIT_MESSAGE_CONTROL, myCommitMessagePanel);
			}
		}

		@Override
		protected String getDimensionServiceKey() {
			return "Gitee.UntrackedFilesDialog";
		}
	}

	private static class GiteeInfo {
		@NotNull private final GiteeUserDetailed myUser;
		@NotNull private final HashSet<String> myRepositoryNames;

		GiteeInfo(@NotNull GiteeUserDetailed user, @NotNull HashSet<String> repositoryNames) {
			myUser = user;
			myRepositoryNames = repositoryNames;
		}

		@NotNull
		public GiteeUserDetailed getUser() {
			return myUser;
		}

		@NotNull
		public HashSet<String> getRepositoryNames() {
			return myRepositoryNames;
		}
	}
}
