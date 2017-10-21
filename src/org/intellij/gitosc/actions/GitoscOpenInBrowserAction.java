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
package org.intellij.gitosc.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsDataKeys;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vcs.history.VcsFileRevision;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.vcs.log.CommitId;
import com.intellij.vcs.log.VcsLog;
import com.intellij.vcs.log.VcsLogDataKeys;
import com.intellij.vcsUtil.VcsUtil;
import git4idea.GitFileRevision;
import git4idea.GitRevisionNumber;
import git4idea.GitUtil;
import git4idea.history.GitHistoryUtils;
import git4idea.repo.GitRepository;
import org.intellij.gitosc.GitoscBundle;
import org.intellij.gitosc.GitoscConstants;
import org.intellij.gitosc.api.GitoscFullPath;
import org.intellij.gitosc.icons.GitoscIcons;
import org.intellij.gitosc.util.GitoscNotifications;
import org.intellij.gitosc.util.GitoscUrlUtil;
import org.intellij.gitosc.util.GitoscUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static org.intellij.gitosc.GitoscConstants.LOG;


/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/GithubOpenInBrowserAction.java
 * @author JetBrains s.r.o.
 * @author oleg
 * @date 12/10/10
 */
public class GitoscOpenInBrowserAction extends DumbAwareAction {
	public static final String CANNOT_OPEN_IN_BROWSER = "Cannot open in browser";

	protected GitoscOpenInBrowserAction() {
		super(GitoscBundle.message2("gitosc.open.in.browser.title"), GitoscBundle.message2("gitosc.open.in.browser.desc"), GitoscIcons.GITOSC_SMALL);
	}

	@Override
	public void update(final AnActionEvent e) {
		CommitData data = getData(e);
		e.getPresentation().setEnabled(data != null && (data.revisionHash != null || data.virtualFile != null));
		e.getPresentation().setVisible(data != null);
	}

	@Override
	public void actionPerformed(final AnActionEvent e) {
		CommitData data = getData(e);
		assert data != null;
		assert data.revisionHash != null || data.virtualFile != null;

		if(data.revisionHash != null){
			openCommitInBrowser(data.project, data.repository, data.revisionHash);
		}else {
			Editor editor = e.getData(CommonDataKeys.EDITOR);
			openFileInBrowser(data.project, data.repository, data.virtualFile, editor);
		}
	}

	@Nullable
	private static String makeUrlToOpen(@Nullable Editor editor, @NotNull String relativePath, @NotNull String branch, @NotNull String gitoscRemoteUrl) {
		final StringBuilder builder = new StringBuilder();
		final String gitoscRepoUrl = GitoscUrlUtil.makeGitoscRepoUrlFromRemoteUrl(gitoscRemoteUrl);
		if (gitoscRepoUrl == null) {
			return null;
		}
		if (StringUtil.isEmptyOrSpaces(relativePath)) {
			builder.append(gitoscRepoUrl).append("/tree/").append(branch);
		} else {
			builder.append(gitoscRepoUrl).append("/blob/").append(branch).append('/').append(relativePath);
		}

		if (editor != null && editor.getDocument().getLineCount() >= 1) {
			// lines are counted internally from 0, but from 1 on gitosc
			SelectionModel selectionModel = editor.getSelectionModel();
			final int begin = editor.getDocument().getLineNumber(selectionModel.getSelectionStart()) + 1;
			final int selectionEnd = selectionModel.getSelectionEnd();
			int end = editor.getDocument().getLineNumber(selectionEnd) + 1;
			if (editor.getDocument().getLineStartOffset(end - 1) == selectionEnd) {
				end -= 1;
			}
			builder.append("#L").append(begin).append("-L").append(end);
		}

		return builder.toString();
	}

	@Nullable
	private static String getCurrentFileRevisionHash(@NotNull final Project project, @NotNull final VirtualFile file) {
		final Ref<GitRevisionNumber> ref = new Ref<GitRevisionNumber>();
		ProgressManager.getInstance().run(new Task.Modal(project, "Getting last revision", true) {
			@Override
			public void run(@NotNull ProgressIndicator indicator) {
				try {
					ref.set((GitRevisionNumber) GitHistoryUtils.getCurrentRevision(project, VcsUtil.getFilePath(file), "HEAD"));
				} catch (VcsException e) {
					LOG.warn(e);
				}
			}

			@Override
			public void onCancel() {
				throw new ProcessCanceledException();
			}
		});
		if (ref.isNull()) return null;
		return ref.get().getRev();
	}

	@Nullable
	protected CommitData getData(AnActionEvent e) {
		CommitData data = getDataFromHistory(e);
		if (data == null) data = getDataFromLog(e);
		if (data == null) data = getDataFromVirtualFile(e);
		return data;
	}

	protected static void openCommitInBrowser(@NotNull Project project, @NotNull GitRepository repository, @NotNull String revisionHash) {
		String url = GitoscUtil.findGitoscRemoteUrl(repository);
		if (url == null) {
			GitoscConstants.LOG.info(String.format("Repository is not under GitOSC. Root: %s, Remotes: %s", repository.getRoot(),
				GitUtil.getPrintableRemotes(repository.getRemotes())));
			return;
		}
		GitoscFullPath userAndRepository = GitoscUrlUtil.getUserAndRepositoryFromRemoteUrl(url);
		if (userAndRepository == null) {
			GitoscNotifications
				.showError(project, GitoscOpenInBrowserAction.CANNOT_OPEN_IN_BROWSER, "Cannot extract info about repository: " + url);
			return;
		}

		String gitoscUrl = GitoscUrlUtil.getGitoscHost() + '/' + userAndRepository.getUser() + '/'
			+ userAndRepository.getRepository() + "/commit/" + revisionHash;
		BrowserUtil.browse(gitoscUrl);
	}

	private static void openFileInBrowser(@NotNull Project project, @NotNull GitRepository repository, @NotNull VirtualFile virtualFile,
	                                      @Nullable Editor editor) {
		String githubRemoteUrl = GitoscUtil.findGitoscRemoteUrl(repository);
		if (githubRemoteUrl == null) {
			LOG.info(String.format("Repository is not under GitOSC. Root: %s, Remotes: %s", repository.getRoot(),
				GitUtil.getPrintableRemotes(repository.getRemotes())));
			return;
		}

		String relativePath = VfsUtilCore.getRelativePath(virtualFile, repository.getRoot());
		if (relativePath == null) {
			GitoscNotifications.showError(project, CANNOT_OPEN_IN_BROWSER, "File is not under repository root",
				"Root: " + repository.getRoot().getPresentableUrl() + ", file: " + virtualFile.getPresentableUrl());
			return;
		}

		String hash = getCurrentFileRevisionHash(project, virtualFile);
		if (hash == null) {
			GitoscNotifications.showError(project, CANNOT_OPEN_IN_BROWSER, "Can't get last revision.");
			return;
		}

		String githubUrl = makeUrlToOpen(editor, relativePath, hash, githubRemoteUrl);
		if (githubUrl != null) BrowserUtil.browse(githubUrl);
	}

	@Nullable
	private static CommitData getDataFromHistory(AnActionEvent e) {
		Project project = e.getData(CommonDataKeys.PROJECT);
		FilePath filePath = e.getData(VcsDataKeys.FILE_PATH);
		VcsFileRevision fileRevision = e.getData(VcsDataKeys.VCS_FILE_REVISION);
		if (project == null || filePath == null || fileRevision == null) return null;

		if (!(fileRevision instanceof GitFileRevision)) return null;

		GitRepository repository = GitUtil.getRepositoryManager(project).getRepositoryForFile(filePath);
		if (repository == null || !GitoscUtil.isRepositoryOnGitosc(repository)) return null;

		return new CommitData(project, repository, fileRevision.getRevisionNumber().asString());
	}

	@Nullable
	private static CommitData getDataFromLog(AnActionEvent e) {
		Project project = e.getData(CommonDataKeys.PROJECT);
		VcsLog log = e.getData(VcsLogDataKeys.VCS_LOG);
		if (project == null || log == null) return null;

		List<CommitId> selectedCommits = log.getSelectedCommits();
		if (selectedCommits.size() != 1) return null;

		CommitId commit = ContainerUtil.getFirstItem(selectedCommits);
		if (commit == null) return null;

		GitRepository repository = GitUtil.getRepositoryManager(project).getRepositoryForRoot(commit.getRoot());
		if (repository == null || !GitoscUtil.isRepositoryOnGitosc(repository)) return null;

		return new CommitData(project, repository, commit.getHash().asString());
	}

	@Nullable
	private static CommitData getDataFromVirtualFile(AnActionEvent e) {
		Project project = e.getData(CommonDataKeys.PROJECT);
		VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
		if (project == null || virtualFile == null) return null;

		GitRepository gitRepository = GitUtil.getRepositoryManager(project).getRepositoryForFile(virtualFile);
		if (gitRepository == null || !GitoscUtil.isRepositoryOnGitosc(gitRepository)) return null;

		ChangeListManager changeListManager = ChangeListManager.getInstance(project);
		if (changeListManager.isUnversioned(virtualFile)) return new CommitData(project, gitRepository);

		Change change = changeListManager.getChange(virtualFile);
		if (change != null && change.getType() == Change.Type.NEW) return new CommitData(project, gitRepository);

		return new CommitData(project, gitRepository, virtualFile);
	}

	protected static class CommitData {
		@NotNull private final Project project;
		@NotNull private final GitRepository repository;
		@Nullable private final String revisionHash;
		@Nullable private final VirtualFile virtualFile;

		public CommitData(@NotNull Project project, @NotNull GitRepository repository) {
			this.project = project;
			this.repository = repository;
			this.revisionHash = null;
			this.virtualFile = null;
		}


		public CommitData(@NotNull Project project, @NotNull GitRepository repository, @Nullable String revisionHash) {
			this.project = project;
			this.repository = repository;
			this.revisionHash = revisionHash;
			this.virtualFile = null;
		}

		public CommitData(@NotNull Project project, @NotNull GitRepository repository, @Nullable VirtualFile virtualFile) {
			this.project = project;
			this.repository = repository;
			this.revisionHash = null;
			this.virtualFile = virtualFile;
		}
	}
}