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
package org.intellij.gitee.util;

import com.intellij.concurrency.JobScheduler;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ThrowableConvertor;
import git4idea.DialogManager;
import git4idea.GitUtil;
import git4idea.commands.GitCommand;
import git4idea.commands.GitSimpleHandler;
import git4idea.config.GitVcsApplicationSettings;
import git4idea.config.GitVersion;
import git4idea.i18n.GitBundle;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.intellij.gitee.GiteeConstants;
import org.intellij.gitee.api.GiteeApiUtil;
import org.intellij.gitee.api.GiteeConnection;
import org.intellij.gitee.api.data.GiteeAuthorization;
import org.intellij.gitee.api.data.GiteeUserDetailed;
import org.intellij.gitee.exceptions.GiteeAuthenticationException;
import org.intellij.gitee.exceptions.GiteeOperationCanceledException;
import org.intellij.gitee.ui.GiteeLoginDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/util/GithubUtil.java
 * @author JetBrains s.r.o.
 * @author oleg
 * @author Kirill Likhodedov
 * @author Aleksey Pivovarov
 */
public class GiteeUtil {
	public static boolean testGitExecutable(final Project project) {
		final GitVcsApplicationSettings settings = GitVcsApplicationSettings.getInstance();
		final String executable = settings.getPathToGit();
		final GitVersion version;
		try {
			version = GitVersion.identifyVersion(executable);
		}
		catch (Exception e) {
			// Error Running Git
			GiteeNotifications.showErrorDialog(project, GitBundle.getString("find.git.error.title"), e);
			return false;
		}

		if (!version.isSupported()) {
			// 1 -> <html><tt>{0}</tt><br>This version is unsupported, and some plugin functionality could fail to work.<br>The minimal supported version is <em>{1}</em>.</html>
			// 2 -> Git executed successfully
			GiteeNotifications.showWarningDialog(project, GitBundle.message("find.git.unsupported.message", version.toString(), GitVersion.MIN),
				GitBundle.getString("find.git.success.title"));
			return false;
		}
		return true;
	}

	/**
	 * Open in browser action event set visible and enabled.
	 * */
	public static void setVisibleEnabled(AnActionEvent e, boolean visible, boolean enabled) {
		e.getPresentation().setVisible(visible);
		e.getPresentation().setEnabled(enabled);
	}

	//====================================================================================
	// gitee repo
	//====================================================================================

	/**
	 * 判断是否有关联GitOSC的远程仓库
	 * */
	public static boolean isRepositoryOnGitosc(@NotNull GitRepository repository) {
		return findGitoscRemoteUrl(repository) != null;
	}

	@Nullable
	public static String findGitoscRemoteUrl(@NotNull GitRepository repository) {
		Pair<GitRemote, String> remote = findGitoscRemote(repository);
		if (remote == null) {
			return null;
		}
		return remote.getSecond();
	}

	@Nullable
	public static Pair<GitRemote, String> findGitoscRemote(@NotNull GitRepository repository) {
		Pair<GitRemote, String> giteeRemote = null;

		for (GitRemote gitRemote : repository.getRemotes()) {
			for (String remoteUrl : gitRemote.getUrls()) {
				if (GiteeUrlUtil.isGitoscUrl(remoteUrl)) {
					final String remoteName = gitRemote.getName();
					if ("gitee".equals(remoteName) || "origin".equals(remoteName)) {
						return Pair.create(gitRemote, remoteUrl);
					}
					if (giteeRemote == null) {
						giteeRemote = Pair.create(gitRemote, remoteUrl);
					}
					break;
				}
			}
		}
		return giteeRemote;
	}

	@Nullable
	public static GitRepository getGitRepository(@NotNull Project project, @Nullable VirtualFile file){
		GitRepositoryManager manager = GitUtil.getRepositoryManager(project);
		List<GitRepository> repositories = manager.getRepositories();

		if(repositories.size() == 0){
			return null;
		}

		if(repositories.size() == 1){
			return repositories.get(0);
		}

		if(file != null){
			GitRepository repository = manager.getRepositoryForFile(file);
			if(repository != null){
				return repository;
			}
		}
		return manager.getRepositoryForFile(project.getBaseDir());
	}

	/**
	 * 添加GitOSC远程仓库地址
	 * */
	public static boolean addGitoscRemote(@NotNull Project project,
	                                      @NotNull GitRepository repository,
	                                      @NotNull String remote,
	                                      @NotNull String url) {
		final GitSimpleHandler handler = new GitSimpleHandler(project, repository.getRoot(), GitCommand.REMOTE);
		handler.setSilent(true);

		try {
			handler.addParameters("add", remote, url);
			handler.run();
			if (handler.getExitCode() != 0) {
				GiteeNotifications.showError(project, "Can't add remote", "Failed to add Gitee remote: '" + url + "'. " + handler.getStderr());
				return false;
			}
			// catch newly added remote
			repository.update();
			return true;
		}
		catch (VcsException e) {
			GiteeNotifications.showError(project, "Can't add remote", e);
			return false;
		}
	}

	//====================================================================================
	// task run
	//====================================================================================
	// TODO: Consider sharing of GiteeAuthData between actions (as member of GiteeSettings)
	public static <T> T runTask(@NotNull Project project,
	                            @NotNull GiteeAuthDataHolder authHolder,
	                            @NotNull final ProgressIndicator indicator,
	                            @NotNull ThrowableConvertor<GiteeConnection, T, IOException> task) throws IOException {
		return runTask(project, authHolder, indicator, AuthLevel.LOGGED, task);
	}

	public static <T> T runTask(@NotNull Project project,
	                            @NotNull GiteeAuthDataHolder authHolder,
	                            @NotNull final ProgressIndicator indicator,
	                            @NotNull AuthLevel authLevel,
	                            @NotNull ThrowableConvertor<GiteeConnection, T, IOException> task) throws IOException {

		GiteeAuthData auth = authHolder.getAuthData();
		try {
			if (!authLevel.accepts(auth)) {
				throw new GiteeAuthenticationException("Expected other authentication type: " + authLevel);
			}

			final GiteeConnection connection = new GiteeConnection(auth, true);
			ScheduledFuture<?> future = null;

			try {
				future = addCancellationListener(indicator, connection);
				return task.convert(connection);
			}
			finally {
				connection.close();
				if (future != null) future.cancel(true);
			}
		}
		catch (GiteeAuthenticationException e) {
//			getValidAuthData(project, authHolder, indicator, authLevel, auth);
			getRefreshTokenAuthData(project, authHolder, indicator, authLevel, auth);
			return runTask(project, authHolder, indicator, task);
		}
	}

	private static void getRefreshTokenAuthData(@NotNull final Project project,
	                                            @NotNull final GiteeAuthDataHolder authHolder,
	                                            @NotNull final ProgressIndicator indicator,
	                                            @NotNull final AuthLevel authLevel,
	                                            @NotNull final GiteeAuthData oldAuth) throws IOException {

		authHolder.runTransaction(oldAuth, () -> {
			final Ref<Boolean> ok = new Ref<>();
			final Ref<GiteeAuthData> authData = new Ref<>();

			ApplicationManager.getApplication().invokeAndWait(() -> {
				GiteeAuthData.TokenAuth tokenAuth = oldAuth.getTokenAuth();

				if(tokenAuth != null && tokenAuth.isTryRefreshAccessToken()){
					// 尝试刷新access_token
					final GiteeAuthDataHolder authDataHolder = new GiteeAuthDataHolder(GiteeAuthData.createSessionAuth(oldAuth.getHost(), "", "", tokenAuth.getRefreshToken()));

					try{
						GiteeAuthorization authorization = GiteeUtil.computeValueInModalIO(project, GiteeConstants.TITLE_ACCESS_TO_GITEE, indicator2 ->
							GiteeUtil.refreshAuthData(project, authDataHolder, indicator2));

//						GiteeAuthorization authorization = GiteeUtil.computeValueInModalIO(project, GiteeConstants.TITLE_ACCESS_TO_GITEE, indicator2 ->
//							GiteeUtil.runTask(project, authDataHolder, indicator, AuthLevel.basicOnetime(oldAuth.getHost()), GiteeApiUtil::getRefreshAccessToken));

						// 设置刷新后的access_token
						authData.set(GiteeAuthData.createTokenAuth(oldAuth.getHost(), authorization.getAccessToken(), authorization.getRefreshToken()));

						GiteeSettings.getInstance().setAuthData(authData.get(), true);

						ok.set(true);
					}catch(IOException ignore){
						ignore.printStackTrace();

						tokenAuth.setTryRefreshAccessToken(false);
						authData.set(oldAuth);
					}
				}else{
					// refresh_token为空或者已经尝试过刷新access_token
					final GiteeLoginDialog dialog = new GiteeLoginDialog(project, oldAuth, authLevel);
					DialogManager.show(dialog);

					ok.set(dialog.isOK());

					if (ok.get()) {
						authData.set(dialog.getAuthData());
						GiteeSettings.getInstance().setAuthData(authData.get(), dialog.isSavePasswordSelected());
					}
				}
			}, indicator.getModalityState());

			if (!ok.get()) {
				throw new GiteeOperationCanceledException("Can't get valid credentials");
			}

			return authData.get();
		});
	}

	private static void getValidAuthData2(@NotNull final Project project,
	                                      @NotNull final GiteeAuthDataHolder authHolder,
	                                      @NotNull final ProgressIndicator indicator,
	                                      @NotNull final AuthLevel authLevel,
	                                      @NotNull final GiteeAuthData oldAuth) throws GiteeOperationCanceledException {

		authHolder.runTransaction(oldAuth, () -> {
			final GiteeAuthData[] authData = new GiteeAuthData[1];
			final boolean[] ok = new boolean[1];

			ApplicationManager.getApplication().invokeAndWait(() -> {
				GiteeAuthData.SessionAuth sessionAuth = oldAuth.getSessionAuth();
				if(sessionAuth != null && sessionAuth.isTryGetNewAccessToken()){
					// 尝试获取新private_token
					final GiteeAuthDataHolder authDataHolder = new GiteeAuthDataHolder(oldAuth);
					try{
						GiteeUtil.computeValueInModalIO(project, GiteeConstants.TITLE_ACCESS_TO_GITEE, indicator2 ->
							GiteeUtil.checkAuthData(project, authDataHolder, indicator2));

						authData[0] = authDataHolder.getAuthData();

						GiteeSettings.getInstance().setAuthData(authData[0], true);

						ok[0] = true;
					}catch(IOException ignore){
						ignore.printStackTrace();

						sessionAuth.setTryGetNewAccessToken(false);
						authData[0] = oldAuth;
					}
				}else{
					// 帐号密码为空或者已经尝试过重新获取private_token
					final GiteeLoginDialog dialog = new GiteeLoginDialog(project, oldAuth, authLevel);
					DialogManager.show(dialog);
					ok[0] = dialog.isOK();

					if (ok[0]) {
						authData[0] = dialog.getAuthData();
						GiteeSettings.getInstance().setAuthData(authData[0], dialog.isSavePasswordSelected());
					}
				}

			}, indicator.getModalityState());

			if (!ok[0]) {
				throw new GiteeOperationCanceledException("Can't get valid credentials");
			}

			return authData[0];
		});
	}

	private static void getValidAuthData(@NotNull final Project project,
	                                     @NotNull final GiteeAuthDataHolder authHolder,
	                                     @NotNull final ProgressIndicator indicator,
	                                     @NotNull final AuthLevel authLevel,
	                                     @NotNull final GiteeAuthData oldAuth) throws GiteeOperationCanceledException {
		authHolder.runTransaction(oldAuth, () -> {
			final GiteeAuthData[] authData = new GiteeAuthData[1];
			ApplicationManager.getApplication().invokeAndWait(() -> {
				GiteeLoginDialog dialog = new GiteeLoginDialog(project, oldAuth, authLevel);
				DialogManager.show(dialog);

				if (dialog.isOK()) {
					authData[0] = dialog.getAuthData();

					if (!authLevel.isOnetime()) {
						GiteeSettings.getInstance().setAuthData(authData[0], dialog.isSavePasswordSelected());
					}
				}
			}, indicator.getModalityState());

			if (authData[0] == null) throw new GiteeOperationCanceledException("Can't get valid credentials");
			return authData[0];
		});
	}

	@NotNull
	public static String getErrorTextFromException(@NotNull Exception e) {
		if (e instanceof UnknownHostException) {
			return "Unknown host: " + e.getMessage();
		}
		return e.getMessage();
	}

	//====================================================================================
	// compute value in modal
	//====================================================================================
	public static <T> T computeValueInModalIO(@NotNull Project project,
	                                          @NotNull String caption,
	                                          @NotNull final ThrowableConvertor<ProgressIndicator, T, IOException> task) throws IOException {
		return ProgressManager.getInstance().run(new Task.WithResult<T, IOException>(project, caption, true) {
			@Override
			protected T compute(@NotNull ProgressIndicator indicator) throws IOException {
				return task.convert(indicator);
			}
		});
	}

	//====================================================================================
	// Auth
	//====================================================================================
	@NotNull
	public static GiteeAuthDataHolder getValidAuthDataHolderFromConfig(@NotNull Project project,
	                                                                   @NotNull AuthLevel authLevel,
	                                                                   @NotNull ProgressIndicator indicator) throws IOException {
		GiteeAuthData auth = GiteeAuthData.createFromSettings();
		GiteeAuthDataHolder authHolder = new GiteeAuthDataHolder(auth);

		try {
			if (!authLevel.accepts(auth)) throw new GiteeAuthenticationException("Expected other authentication type: " + authLevel);
			checkAuthData(project, authHolder, indicator);
			return authHolder;
		}
		catch (GiteeAuthenticationException e) {
			getValidAuthData(project, authHolder, indicator, authLevel, auth);
			return authHolder;
		}
	}

	@NotNull
	public static GiteeAuthorization refreshAuthData(@NotNull Project project,
	                                                 @NotNull GiteeAuthDataHolder authHolder,
	                                                 @NotNull ProgressIndicator indicator) throws IOException {

		GiteeAuthData auth = authHolder.getAuthData();

		if (StringUtil.isEmptyOrSpaces(auth.getHost())) {
			throw new GiteeAuthenticationException("Target host not defined");
		}

		try {
			new URI(auth.getHost());
		}
		catch (URISyntaxException e) {
			throw new GiteeAuthenticationException("Invalid host URL");
		}

		switch (auth.getAuthType()) {
			case SESSION:
				GiteeAuthData.SessionAuth sessionAuth = auth.getSessionAuth();
				assert sessionAuth != null;
				if(StringUtil.isEmptyOrSpaces(sessionAuth.getAccessToken())){
					throw new GiteeAuthenticationException("Empty Refresh Token");
				}
				break;
			default:
				throw new GiteeAuthenticationException(auth.getAuthType() + " connection not allowed");
		}

		return testRefreshAuth(project, authHolder, indicator);
	}

	@NotNull
	private static GiteeAuthorization testRefreshAuth(@NotNull Project project,
	                                                  @NotNull GiteeAuthDataHolder authHolder,
	                                                  @NotNull final ProgressIndicator indicator) throws IOException {
		GiteeAuthData auth = authHolder.getAuthData();

		final GiteeConnection connection = new GiteeConnection(auth, true);
		ScheduledFuture<?> future = null;

		try {
			future = addCancellationListener(indicator, connection);
			return GiteeApiUtil.getRefreshAccessToken(connection);
		}
		finally {
			connection.close();
			if (future != null) future.cancel(true);
		}
	}

	@NotNull
	public static GiteeAuthorization loginAuthData(@NotNull Project project,
	                                               @NotNull GiteeAuthDataHolder authHolder,
	                                               @NotNull ProgressIndicator indicator) throws IOException {

		GiteeAuthData auth = authHolder.getAuthData();

		if (StringUtil.isEmptyOrSpaces(auth.getHost())) {
			throw new GiteeAuthenticationException("Target host not defined");
		}

		try {
			new URI(auth.getHost());
		}
		catch (URISyntaxException e) {
			throw new GiteeAuthenticationException("Invalid host URL");
		}

		switch (auth.getAuthType()) {
			case SESSION:
				GiteeAuthData.SessionAuth sessionAuth = auth.getSessionAuth();
				assert sessionAuth != null;
				if(StringUtil.isEmptyOrSpaces(sessionAuth.getLogin()) || StringUtil.isEmptyOrSpaces(sessionAuth.getPassword())){
					throw new GiteeAuthenticationException("Empty login or password");
				}
				break;
			case TOKEN:
				GiteeAuthData.TokenAuth tokenAuth = auth.getTokenAuth();
				assert tokenAuth != null;
				if (StringUtil.isEmptyOrSpaces(tokenAuth.getToken())) {
					throw new GiteeAuthenticationException("Empty token");
				}
				break;
			default:
				throw new GiteeAuthenticationException(auth.getAuthType() + " connection not allowed");
		}

		return testLogin(project, authHolder, indicator);
	}

	@NotNull
	private static GiteeAuthorization testLogin(@NotNull Project project,
	                                            @NotNull GiteeAuthDataHolder authHolder,
	                                            @NotNull final ProgressIndicator indicator) throws IOException {
		GiteeAuthData auth = authHolder.getAuthData();

		final GiteeConnection connection = new GiteeConnection(auth, true);
		ScheduledFuture<?> future = null;

		try {
			future = addCancellationListener(indicator, connection);
			return GiteeApiUtil.getAuthorization(connection);
		}
		finally {
			connection.close();
			if (future != null) future.cancel(true);
		}
	}


	@NotNull
	public static GiteeUserDetailed checkAuthData(@NotNull Project project,
	                                              @NotNull GiteeAuthDataHolder authHolder,
	                                              @NotNull ProgressIndicator indicator) throws IOException {

		GiteeAuthData auth = authHolder.getAuthData();

		if (StringUtil.isEmptyOrSpaces(auth.getHost())) {
			throw new GiteeAuthenticationException("Target host not defined");
		}

		try {
			new URI(auth.getHost());
		}
		catch (URISyntaxException e) {
			throw new GiteeAuthenticationException("Invalid host URL");
		}

		switch (auth.getAuthType()) {
			case SESSION:
				GiteeAuthData.SessionAuth sessionAuth = auth.getSessionAuth();
				assert sessionAuth != null;
				if(StringUtil.isEmptyOrSpaces(sessionAuth.getLogin()) || StringUtil.isEmptyOrSpaces(sessionAuth.getPassword())){
					throw new GiteeAuthenticationException("Empty login or password");
				}
				break;
			case TOKEN:
				GiteeAuthData.TokenAuth tokenAuth = auth.getTokenAuth();
				assert tokenAuth != null;
				if (StringUtil.isEmptyOrSpaces(tokenAuth.getToken())) {
					throw new GiteeAuthenticationException("Empty token");
				}
				break;
			case ANONYMOUS:
				throw new GiteeAuthenticationException("Anonymous connection not allowed");
		}

		return testConnection(project, authHolder, indicator);
	}

	@NotNull
	private static GiteeUserDetailed testConnection(@NotNull Project project,
	                                                @NotNull GiteeAuthDataHolder authHolder,
	                                                @NotNull final ProgressIndicator indicator) throws IOException {
		GiteeAuthData auth = authHolder.getAuthData();

		final GiteeConnection connection = new GiteeConnection(auth, true);
		ScheduledFuture<?> future = null;

		try {
			future = addCancellationListener(indicator, connection);
			return GiteeApiUtil.getCurrentUserDetailed(connection);
		}
		finally {
			connection.close();
			if (future != null) future.cancel(true);
		}
	}

	//====================================================================================
	// Cancellation Listener
	//====================================================================================
	@NotNull
	private static ScheduledFuture<?> addCancellationListener(@NotNull Runnable run) {
		return JobScheduler.getScheduler().scheduleWithFixedDelay(run, 1000, 300, TimeUnit.MILLISECONDS);
	}

	@NotNull
	private static ScheduledFuture<?> addCancellationListener(@NotNull final ProgressIndicator indicator,
	                                                          @NotNull final GiteeConnection connection) {
		return addCancellationListener(() -> {
			if (indicator.isCanceled()) connection.abort();
		});
	}
}
