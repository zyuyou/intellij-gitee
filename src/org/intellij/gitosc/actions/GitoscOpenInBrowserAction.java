/*
 * Copyright 2016 码云
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
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vcs.changes.Change;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.vcsUtil.VcsUtil;
import git4idea.GitRevisionNumber;
import git4idea.GitUtil;
import git4idea.history.GitHistoryUtils;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.intellij.gitosc.GitoscBundle;
import org.intellij.gitosc.icons.GitoscIcons;
import org.intellij.gitosc.util.GitoscNotifications;
import org.intellij.gitosc.util.GitoscUrlUtil;
import org.intellij.gitosc.util.GitoscUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.intellij.gitosc.GitoscConstants.LOG;
import static org.intellij.gitosc.util.GitoscUtil.setVisibleEnabled;


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
		Project project = e.getData(CommonDataKeys.PROJECT);
		VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
		if (project == null || project.isDefault() || virtualFile == null) {
			setVisibleEnabled(e, false, false);
			return;
		}
		GitRepositoryManager manager = GitUtil.getRepositoryManager(project);

		final GitRepository gitRepository = manager.getRepositoryForFile(virtualFile);
		if (gitRepository == null) {
			setVisibleEnabled(e, false, false);
			return;
		}

		if (!GitoscUtil.isRepositoryOnGitosc(gitRepository)) {
			setVisibleEnabled(e, false, false);
			return;
		}

		ChangeListManager changeListManager = ChangeListManager.getInstance(project);
		if (changeListManager.isUnversioned(virtualFile)) {
			setVisibleEnabled(e, true, false);
			return;
		}

		Change change = changeListManager.getChange(virtualFile);
		if (change != null && change.getType() == Change.Type.NEW) {
			setVisibleEnabled(e, true, false);
			return;
		}

		setVisibleEnabled(e, true, true);
	}

	@Override
	public void actionPerformed(final AnActionEvent e) {
		final Project project = e.getData(CommonDataKeys.PROJECT);
		final VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
		final Editor editor = e.getData(CommonDataKeys.EDITOR);
		if (virtualFile == null || project == null || project.isDisposed()) {
			return;
		}

		String urlToOpen = getGitoscUrl(project, virtualFile, editor);
		if (urlToOpen != null) {
			BrowserUtil.browse(urlToOpen);
		}
	}

	@Nullable
	private static String getGitoscUrl(@NotNull Project project, @NotNull VirtualFile virtualFile, @Nullable Editor editor) {
		GitRepositoryManager manager = GitUtil.getRepositoryManager(project);
		final GitRepository repository = manager.getRepositoryForFile(virtualFile);
		if (repository == null) {
			StringBuilder details = new StringBuilder("file: " + virtualFile.getPresentableUrl() + "; Git repositories: ");
			for (GitRepository repo : manager.getRepositories()) {
				details.append(repo.getPresentableUrl()).append("; ");
			}
			GitoscNotifications.showError(project, CANNOT_OPEN_IN_BROWSER, "Can't find git repository", details.toString());
			return null;
		}

		final String gitoscRemoteUrl = GitoscUtil.findGitoscRemoteUrl(repository);
		if (gitoscRemoteUrl == null) {
			GitoscNotifications.showError(project, CANNOT_OPEN_IN_BROWSER, "Can't find githosc remote");
			return null;
		}

		String relativePath = VfsUtilCore.getRelativePath(virtualFile, repository.getRoot());
		if (relativePath == null) {
			GitoscNotifications.showError(project, CANNOT_OPEN_IN_BROWSER, "File is not under repository root", "Root: " + repository.getRoot().getPresentableUrl() + ", file: " + virtualFile.getPresentableUrl());
			return null;
		}

		String hash = getCurrentFileRevisionHash(project, virtualFile);
		if (hash != null) {
			return makeUrlToOpen(editor, relativePath, hash, gitoscRemoteUrl);
		}

		GitoscNotifications.showError(project, CANNOT_OPEN_IN_BROWSER, "Can't get last revision.");
		return null;
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
}