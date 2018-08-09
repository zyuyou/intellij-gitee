/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.gitee.actions;

import com.gitee.api.GiteeApiRequestExecutor;
import com.gitee.api.GiteeApiRequestExecutorManager;
import com.gitee.api.GiteeApiRequests;
import com.gitee.api.GiteeServerPath;
import com.gitee.api.requests.GiteeGistRequest.FileContent;
import com.gitee.authentication.GiteeAuthenticationManager;
import com.gitee.icons.GiteeIcons;
import com.gitee.ui.GiteeCreateGistDialog;
import com.gitee.util.GiteeNotifications;
import com.gitee.util.GiteeSettings;
import com.gitee.util.GiteeUtil;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.changes.ChangeListManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author oleg
 * @date 9/27/11
 */
public class GiteeCreateGistAction extends DumbAwareAction {
	private static final Logger LOG = GiteeUtil.LOG;
	private static final String FAILED_TO_CREATE_GIST = "Can't create Gist";

	protected GiteeCreateGistAction() {
		super("Create Gist...", "Create Gitee Gist", GiteeIcons.Gitee_icon);
	}

	@Override
	public void update(@NotNull final AnActionEvent e) {
		Project project = e.getData(CommonDataKeys.PROJECT);
		if (project == null || project.isDefault()) {
			e.getPresentation().setVisible(false);
			e.getPresentation().setEnabled(false);
			return;
		}

		Editor editor = e.getData(CommonDataKeys.EDITOR);
		VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
		VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);

		if ((editor == null && file == null && files == null) || (editor != null && editor.getDocument().getTextLength() == 0)) {
			e.getPresentation().setEnabledAndVisible(false);
			return;
		}

		e.getPresentation().setEnabledAndVisible(true);
	}

	@Override
	public void actionPerformed(@NotNull final AnActionEvent e) {
		final Project project = e.getData(CommonDataKeys.PROJECT);
		if (project == null || project.isDefault()) {
			return;
		}

		final Editor editor = e.getData(CommonDataKeys.EDITOR);
		final VirtualFile file = e.getData(CommonDataKeys.VIRTUAL_FILE);
		final VirtualFile[] files = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY);

		if (editor == null && file == null && files == null) {
			return;
		}

		createGistAction(project, editor, file, files);
	}

	private static void createGistAction(@NotNull final Project project,
	                                     @Nullable final Editor editor,
	                                     @Nullable final VirtualFile file,
	                                     @Nullable final VirtualFile[] files) {

		GiteeAuthenticationManager authManager = GiteeAuthenticationManager.getInstance();
		if (!authManager.ensureHasAccounts(project)) return;

		GiteeSettings settings = GiteeSettings.getInstance();

		// Ask for description and other params
		GiteeCreateGistDialog dialog = new GiteeCreateGistDialog(project, authManager.getAccounts(), authManager.getDefaultAccount(project), getFileName(editor, files), settings.isPrivateGist(), settings.isOpenInBrowserGist(), settings.isCopyURLGist());
		if (!dialog.showAndGet()) {
			return;
		}
		settings.setPrivateGist(dialog.isSecret());
		settings.setOpenInBrowserGist(dialog.isOpenInBrowser());
		settings.setCopyURLGist(dialog.isCopyURL());

		GiteeApiRequestExecutor requestExecutor = GiteeApiRequestExecutorManager.getInstance().getExecutor(dialog.getAccount(), project);
		if (requestExecutor == null) return;

		GiteeServerPath server = dialog.getAccount().getServer();

		final Ref<String> url = new Ref<>();
		new Task.Backgroundable(project, "Creating Gist...") {
			@Override
			public void run(@NotNull ProgressIndicator indicator) {
				List<FileContent> contents = collectContents(project, editor, file, files);
				if (contents.isEmpty()) return;

				String gistUrl = createGist(project, requestExecutor, indicator, server, contents, dialog.isSecret(), dialog.getDescription(), dialog.getFileName());
				url.set(gistUrl);
			}

			@Override
			public void onSuccess() {
				if (url.isNull()) {
					return;
				}
				if (dialog.isCopyURL()) {
					StringSelection stringSelection = new StringSelection(url.get());
					CopyPasteManager.getInstance().setContents(stringSelection);
				}
				if (dialog.isOpenInBrowser()) {
					BrowserUtil.browse(url.get());
				} else {
					GiteeNotifications.showInfoURL(project, "Gist Created Successfully", "Your gist url", url.get());
				}
			}
		}.queue();
	}

	@Nullable
	private static String getFileName(@Nullable Editor editor, @Nullable VirtualFile[] files) {
		if (files != null && files.length == 1 && !files[0].isDirectory()) {
			return files[0].getName();
		}
		if (editor != null) {
			return "";
		}
		return null;
	}

	@NotNull
	static List<FileContent> collectContents(@NotNull Project project,
	                                         @Nullable Editor editor,
	                                         @Nullable VirtualFile file,
	                                         @Nullable VirtualFile[] files) {

		if (editor != null) {
			String content = getContentFromEditor(editor);
			if (content == null) {
				return Collections.emptyList();
			}
			if (file != null) {
				return Collections.singletonList(new FileContent(file.getName(), content));
			} else {
				return Collections.singletonList(new FileContent("", content));
			}
		}
		if (files != null) {
			List<FileContent> contents = new ArrayList<>();
			for (VirtualFile vf : files) {
				contents.addAll(getContentFromFile(vf, project, null));
			}
			return contents;
		}

		if (file != null) {
			return getContentFromFile(file, project, null);
		}

		LOG.error("File, files and editor can't be null all at once!");
		throw new IllegalStateException("File, files and editor can't be null all at once!");
	}

	@Nullable
	static String createGist(@NotNull Project project,
	                         @NotNull GiteeApiRequestExecutor executor,
	                         @NotNull ProgressIndicator indicator,
	                         @NotNull GiteeServerPath server,
	                         @NotNull List<FileContent> contents,
	                         final boolean isSecret,
	                         @NotNull final String description,
	                         @Nullable String filename) {

		if (contents.isEmpty()) {
			GiteeNotifications.showWarning(project, FAILED_TO_CREATE_GIST, "Can't create empty gist");
			return null;
		}

		if (contents.size() == 1 && filename != null) {
			FileContent entry = contents.iterator().next();
			contents = Collections.singletonList(new FileContent(filename, entry.getContent()));
		}

		try {
			return executor.execute(indicator, GiteeApiRequests.Gists.create(server, contents, description, !isSecret)).getHtmlUrl();
		} catch (IOException e) {
			GiteeNotifications.showError(project, FAILED_TO_CREATE_GIST, e);
			return null;
		}
	}

	@Nullable
	private static String getContentFromEditor(@NotNull final Editor editor) {
		String text = ReadAction.compute(() -> editor.getSelectionModel().getSelectedText());
		if (text == null) {
			text = editor.getDocument().getText();
		}

		if (StringUtil.isEmptyOrSpaces(text)) {
			return null;
		}
		return text;
	}

	@NotNull
	private static List<FileContent> getContentFromFile(@NotNull final VirtualFile file, @NotNull Project project, @Nullable String prefix) {
		if (file.isDirectory()) {
			return getContentFromDirectory(file, project, prefix);
		}

		if (file.getFileType().isBinary()) {
			GiteeNotifications.showWarning(project, FAILED_TO_CREATE_GIST, "Can't upload binary file: " + file);
			return Collections.emptyList();
		}

		String content = ReadAction.compute(() -> {
			try {
				Document document = FileDocumentManager.getInstance().getDocument(file);
				if (document != null) {
					return document.getText();
				} else {
					return new String(file.contentsToByteArray(), file.getCharset());
				}
			} catch (IOException e) {
				LOG.info("Couldn't read contents of the file " + file, e);
				return null;
			}
		});

		if (content == null) {
			GiteeNotifications.showWarning(project, FAILED_TO_CREATE_GIST, "Couldn't read the contents of the file " + file);
			return Collections.emptyList();
		}

		if (StringUtil.isEmptyOrSpaces(content)) {
			return Collections.emptyList();
		}

		String filename = addPrefix(file.getName(), prefix, false);
		return Collections.singletonList(new FileContent(filename, content));
	}

	@NotNull
	private static List<FileContent> getContentFromDirectory(@NotNull VirtualFile dir, @NotNull Project project, @Nullable String prefix) {
		List<FileContent> contents = new ArrayList<>();
		for (VirtualFile file : dir.getChildren()) {
			if (!isFileIgnored(file, project)) {
				String pref = addPrefix(dir.getName(), prefix, true);
				contents.addAll(getContentFromFile(file, project, pref));
			}
		}
		return contents;
	}

	private static String addPrefix(@NotNull String name, @Nullable String prefix, boolean addTrailingSlash) {
		String pref = prefix == null ? "" : prefix;
		pref += name;
		if (addTrailingSlash) {
			pref += "_";
		}
		return pref;
	}

	private static boolean isFileIgnored(@NotNull VirtualFile file, @NotNull Project project) {
		ChangeListManager manager = ChangeListManager.getInstance(project);
		return manager.isIgnoredFile(file) || FileTypeManager.getInstance().isFileIgnored(file);
	}
}
