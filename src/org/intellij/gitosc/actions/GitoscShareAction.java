package org.intellij.gitosc.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
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
import org.intellij.gitosc.api.*;
import org.intellij.gitosc.exceptions.GitoscStatusCodeException;
import org.intellij.gitosc.icons.GitoscIcons;
import org.intellij.gitosc.ui.GitoscShareDialog;
import org.intellij.gitosc.util.GitoscAuthDataHolder;
import org.intellij.gitosc.util.GitoscNotifications;
import org.intellij.gitosc.util.GitoscUrlUtil;
import org.intellij.gitosc.util.GitoscUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.*;


/**
 * Created by zyuyou on 16/5/27.
 */
public class GitoscShareAction extends DumbAwareAction {
	private static final Logger LOG = GitoscUtil.LOG;

	public GitoscShareAction() {
		super("Share Project on GitOSC", "Easily share project on GitOSC", GitoscIcons.GITOSC_SMALL);
	}

	@Override
	public void actionPerformed(AnActionEvent e) {
		final Project project = e.getData(CommonDataKeys.PROJECT);
		final VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);

		if(project == null || project.isDisposed() || !GitoscUtil.testGitExecutable(project)){
			return;
		}

		shareProjectOnGitosc(project, file);
	}

	public static void shareProjectOnGitosc(@NotNull final Project project, @Nullable final VirtualFile file){
		BasicAction.saveAll();

		// get gitRepository
		final GitRepository gitRepository = GitoscUtil.getGitRepository(project, file);
		final boolean gitDetected = gitRepository != null;
		final VirtualFile root = gitDetected ? gitRepository.getRoot() : project.getBaseDir();

		final GitoscAuthDataHolder authDataHolder = GitoscAuthDataHolder.createFromSettings();

		// check for existing git repo
		Set<String> existingRemotes = Collections.emptySet();
		if(gitDetected){
			final String gitoscRemote = GitoscUtil.findGitoscRemoteUrl(gitRepository);
			if(gitoscRemote != null){
				if(!checkExistingRemote(project, authDataHolder, gitoscRemote)) return;
			}

			existingRemotes = ContainerUtil.map2Set(gitRepository.getRemotes(), GitRemote::getName);
		}

		// get available Gitosc repos with modal progress
		final GitoscInfo gitoscInfo = loadGitoscInfoWithModal(authDataHolder, project);
		if(gitoscInfo == null){
			return;
		}

		// show dialog
		final GitoscShareDialog shareDialog = new GitoscShareDialog(project, gitoscInfo.getRepositoryNames(), existingRemotes, true);
		DialogManager.show(shareDialog);
		if (!shareDialog.isOK()) {
			return;
		}
		final boolean isPrivate = shareDialog.isPrivate();
		final String name = shareDialog.getRepositoryName();
		final String description = shareDialog.getDescription();
		final String remoteName = shareDialog.getRemoteName();


		new Task.Backgroundable(project, "Sharing Project on GitOSC...") {
			@Override
			public void run(@NotNull ProgressIndicator indicator) {
				// create GitOSC repo
				LOG.info("Creating GitOSC repository");
				indicator.setText("Creating GitOSC repository...");
				final String url = createGitoscRepository(project, authDataHolder, indicator, name, description, isPrivate);
				if (url == null) {
					return;
				}
				LOG.info("Successfully created GitOSC repository");

				// creating empty git repo if git is not initialized
				LOG.info("Binding local project with GitOSC");
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
					GitoscNotifications.showError(project, "Failed to create GitOSC Repository", "Can't find Git repository");
					return;
				}

				final String remoteUrl = GitoscUrlUtil.getCloneUrl(gitoscInfo.getUser().getLogin(), name);

				//git remote add origin git@git.oschina.net:login/name.git
				LOG.info("Adding GitOSC as a remote host");
				indicator.setText("Adding GitOSC as a remote host...");
				if (!GitoscUtil.addGitoscRemote(project, repository, remoteName, remoteUrl)) {
					return;
				}

				// create sample commit for binding project
				if (!performFirstCommitIfRequired(project, root, repository, indicator, name, url)) {
					return;
				}

				//git push origin master
				LOG.info("Pushing to gitosc master");
				indicator.setText("Pushing to gitosc master...");
				if (!pushCurrentBranch(project, repository, remoteName, remoteUrl, name, url)) {
					return;
				}

				GitoscNotifications.showInfoURL(project, "Successfully shared project on GitOSC", name, url);
			}
		}.queue();
	}

	@Nullable
	private static String createGitoscRepository(@NotNull Project project,
	                                             @NotNull GitoscAuthDataHolder authHolder,
	                                             @NotNull ProgressIndicator indicator,
	                                             @NotNull final String name,
	                                             @NotNull final String description,
	                                             final boolean isPrivate) {

		try {
			return GitoscUtil.runTask(project, authHolder, indicator, connection ->
				GitoscApiUtil.createRepo(connection, name, description, isPrivate)).getHtmlUrl();
		}
		catch (IOException e) {
			GitoscNotifications.showError(project, "Failed to create GitOSC Repository", e);
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
	                                           @NotNull final GitoscAuthDataHolder authHolder,
	                                           @NotNull String remote) {

		final GitoscFullPath path = GitoscUrlUtil.getUserAndRepositoryFromRemoteUrl(remote);
		if(path == null){
			return GitoscNotifications.showYesNoDialog(project,
				"Project Is Already on GitOSC",
				"Can't connect to repository from configured remote. You could want to check .git config.\n" +
					"Do you want to proceed anyway?");
		}

		try{
			GitoscRepo repo = GitoscUtil.computeValueInModalIO(project, "Access to GitOSC", indicator ->
				GitoscUtil.runTask(project, authHolder, indicator, connection ->
					GitoscApiUtil.getDetailedRepoInfo(connection, path.getUser(), path.getRepository())));

			int result = Messages.showDialog(project,
				"Successfully connected to " + repo.getHtmlUrl() + ".\n" +
					"Do you want to proceed anyway?",
				"Project Is Already on GitOSC",
				new String[]{"Continue", "Open in Browser", Messages.CANCEL_BUTTON}, 2, Messages.getQuestionIcon());

			if(result == 0) return true;

			if (result == 1) {
				BrowserUtil.browse(repo.getHtmlUrl());
			}
			return false;
		}
		catch (GitoscStatusCodeException e) {
			if (e.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
				return GitoscNotifications.showYesNoDialog(project,
					"Project Is Already on GitOSC",
					"Can't connect to repository from configured remote. You could want to check .git config.\n" +
						"Do you want to proceed anyway?");
			}

			GitoscNotifications.showErrorDialog(project, "Failed to Connect to GitOSC", e);
			return false;
		}
		catch (IOException e) {
			GitoscNotifications.showErrorDialog(project, "Failed to Connect to GitOSC", e);
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

			final Ref<GitoscUntrackedFilesDialog> dialogRef = new Ref<GitoscUntrackedFilesDialog>();
			ApplicationManager.getApplication().invokeAndWait(() -> {
				GitoscUntrackedFilesDialog dialog = new GitoscUntrackedFilesDialog(project, allFiles);
				if (!trackedFiles.isEmpty()) {
					dialog.setSelectedFiles(trackedFiles);
				}
				DialogManager.show(dialog);
				dialogRef.set(dialog);
			}, indicator.getModalityState());
			final GitoscUntrackedFilesDialog dialog = dialogRef.get();

			final Collection<VirtualFile> files2commit = dialog.getSelectedFiles();
			if (!dialog.isOK() || files2commit.isEmpty()) {
				GitoscNotifications.showInfoURL(project, "Successfully created empty repository on GitOSC", name, url);
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
			GitoscNotifications.showErrorURL(project, "Can't finish GitOSC sharing process", "Successfully created project ", "'" + name + "'",
				" on GitOSC, but initial commit failed:<br/>" + GitoscUtil.getErrorTextFromException(e), url);
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
			GitoscNotifications.showErrorURL(project, "Can't finish GitOSC sharing process", "Successfully created project ", "'" + name + "'",
				" on GitOSC, but initial push failed: no current branch", url);
			return false;
		}
		GitCommandResult result = git.push(repository, remoteName, remoteUrl, currentBranch.getName(), true);
		if (!result.success()) {
			GitoscNotifications.showErrorURL(project, "Can't finish GitOSC sharing process", "Successfully created project ", "'" + name + "'",
				" on GitOSC, but initial push failed:<br/>" + result.getErrorOutputAsHtmlString(), url);
			return false;
		}
		return true;
	}

	@Nullable
	private static GitoscInfo loadGitoscInfoWithModal(@NotNull final GitoscAuthDataHolder authHolder, @NotNull final Project project) {
		try {
			return GitoscUtil.computeValueInModalIO(project, "Access to GitOSC", indicator -> {
				// get existing gitosc repos (network) and validate auth data
				return GitoscUtil.runTask(project, authHolder, indicator, connection -> {
					// check access to private repos (network)
					GitoscUserDetailed userInfo = GitoscApiUtil.getCurrentUserDetailed(connection, authHolder.getAuthData());

					HashSet<String> names = new HashSet<String>();
					for (GitoscRepo info : GitoscApiUtil.getUserRepos(connection)) {
						names.add(info.getName());
					}
					return new GitoscInfo(userInfo, names);
				});
			});
		}
		catch (IOException e) {
			GitoscNotifications.showErrorDialog(project, "Failed to Connect to GitOSC", e);
			return null;
		}
	}

	public static class GitoscUntrackedFilesDialog extends SelectFilesDialog implements TypeSafeDataProvider {
		@NotNull private final Project myProject;
		private CommitMessage myCommitMessagePanel;

		public GitoscUntrackedFilesDialog(@NotNull Project project, @NotNull List<VirtualFile> untrackedFiles) {
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
			return "GitOSC.UntrackedFilesDialog";
		}
	}

	private static class GitoscInfo {
		@NotNull private final GitoscUserDetailed myUser;
		@NotNull private final HashSet<String> myRepositoryNames;

		GitoscInfo(@NotNull GitoscUserDetailed user, @NotNull HashSet<String> repositoryNames) {
			myUser = user;
			myRepositoryNames = repositoryNames;
		}

		@NotNull
		public GitoscUserDetailed getUser() {
			return myUser;
		}

		@NotNull
		public HashSet<String> getRepositoryNames() {
			return myRepositoryNames;
		}
	}
}
