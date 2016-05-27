package org.intellij.gitosc.util;

import com.google.common.base.Joiner;
import com.intellij.concurrency.JobScheduler;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Consumer;
import com.intellij.util.ThrowableConvertor;
import com.intellij.util.containers.Convertor;
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
import org.intellij.gitosc.api.GitoscApiUtil;
import org.intellij.gitosc.api.GitoscConnection;
import org.intellij.gitosc.api.GitoscUserDetailed;
import org.intellij.gitosc.exceptions.GitoscAuthenticationException;
import org.intellij.gitosc.exceptions.GitoscOperationCanceledException;
import org.intellij.gitosc.ui.GitoscLoginDialog;
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
 * Created by zyuyou on 16/5/25.
 *
 *  https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/util/GithubUtil.java
 */
public class GitoscUtil {
	public static final Logger LOG = Logger.getInstance("gitosc");

	public static final Joiner JOINER = Joiner.on("&").skipNulls();

	public static boolean testGitExecutable(final Project project) {
		final GitVcsApplicationSettings settings = GitVcsApplicationSettings.getInstance();
		final String executable = settings.getPathToGit();
		final GitVersion version;
		try {
			version = GitVersion.identifyVersion(executable);
		}
		catch (Exception e) {
			GitoscNotifications.showErrorDialog(project, GitBundle.getString("find.git.error.title"), e);
			return false;
		}

		if (!version.isSupported()) {
			GitoscNotifications.showWarningDialog(project, GitBundle.message("find.git.unsupported.message", version.toString(), GitVersion.MIN),
				GitBundle.getString("find.git.success.title"));
			return false;
		}
		return true;
	}

	public static void setVisibleEnabled(AnActionEvent e, boolean visible, boolean enabled) {
		e.getPresentation().setVisible(visible);
		e.getPresentation().setEnabled(enabled);
	}

	//====================================================================================
	// gitosc repo
	//====================================================================================
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
		Pair<GitRemote, String> gitoscRemote = null;
		for (GitRemote gitRemote : repository.getRemotes()) {
			for (String remoteUrl : gitRemote.getUrls()) {
				if (GitoscUrlUtil.isGitoscUrl(remoteUrl)) {
					final String remoteName = gitRemote.getName();
					if ("gitosc".equals(remoteName) || "origin".equals(remoteName)) {
						return Pair.create(gitRemote, remoteUrl);
					}
					if (gitoscRemote == null) {
						gitoscRemote = Pair.create(gitRemote, remoteUrl);
					}
					break;
				}
			}
		}
		return gitoscRemote;
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
				GitoscNotifications.showError(project, "Can't add remote", "Failed to add GitOSC remote: '" + url + "'. " + handler.getStderr());
				return false;
			}
			// catch newly added remote
			repository.update();
			return true;
		}
		catch (VcsException e) {
			GitoscNotifications.showError(project, "Can't add remote", e);
			return false;
		}
	}

	//====================================================================================
	// task run
	//====================================================================================
	// TODO: Consider sharing of GitoscAuthData between actions (as member of GitoscSettings)
	public static <T> T runTask(@NotNull Project project,
	                            @NotNull GitoscAuthDataHolder authHolder,
	                            @NotNull final ProgressIndicator indicator,
	                            @NotNull ThrowableConvertor<GitoscConnection, T, IOException> task) throws IOException {
		GitoscAuthData auth = authHolder.getAuthData();
		try {
			final GitoscConnection connection = new GitoscConnection(auth, true);
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
//		catch (GitoscTwoFactorAuthenticationException e) {
//			getTwoFactorAuthData(project, authHolder, indicator, auth);
//			return runTask(project, authHolder, indicator, task);
//		}
		catch (GitoscAuthenticationException e) {
			getValidAuthData(project, authHolder, indicator, auth);
			return runTask(project, authHolder, indicator, task);
		}
	}

	public static void getValidAuthData(@NotNull final Project project,
	                                    @NotNull final GitoscAuthDataHolder authHolder,
	                                    @NotNull final ProgressIndicator indicator,
	                                    @NotNull final GitoscAuthData oldAuth) throws GitoscOperationCanceledException {
		authHolder.runTransaction(oldAuth, () -> {
			final GitoscAuthData[] authData = new GitoscAuthData[1];
			final boolean[] ok = new boolean[1];
			ApplicationManager.getApplication().invokeAndWait(() -> {
				final GitoscLoginDialog dialog = new GitoscLoginDialog(project, oldAuth);
				DialogManager.show(dialog);
				ok[0] = dialog.isOK();

				if (ok[0]) {
					authData[0] = dialog.getAuthData();
					GitoscSettings.getInstance().setAuthData(authData[0], dialog.isSavePasswordSelected());
				}
			}, indicator.getModalityState());
			if (!ok[0]) {
				throw new GitoscOperationCanceledException("Can't get valid credentials");
			}
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

	public static <T> T computeValueInModal(@NotNull Project project,
	                                        @NotNull String caption,
	                                        @NotNull final Convertor<ProgressIndicator, T> task) {
		return computeValueInModal(project, caption, true, task);
	}

	public static <T> T computeValueInModal(@NotNull Project project,
	                                        @NotNull String caption,
	                                        boolean canBeCancelled,
	                                        @NotNull final Convertor<ProgressIndicator, T> task) {
		return ProgressManager.getInstance().run(new Task.WithResult<T, RuntimeException>(project, caption, canBeCancelled) {
			@Override
			protected T compute(@NotNull ProgressIndicator indicator) {
				return task.convert(indicator);
			}
		});
	}

	public static void computeValueInModal(@NotNull Project project,
	                                       @NotNull String caption,
	                                       boolean canBeCancelled,
	                                       @NotNull final Consumer<ProgressIndicator> task) {
		ProgressManager.getInstance().run(new Task.WithResult<Void, RuntimeException>(project, caption, canBeCancelled) {
			@Override
			protected Void compute(@NotNull ProgressIndicator indicator) {
				task.consume(indicator);
				return null;
			}
		});
	}

	//====================================================================================
	// Auth
	//====================================================================================
	@NotNull
	public static GitoscUserDetailed checkAuthData(@NotNull Project project,
	                                               @NotNull GitoscAuthDataHolder authHolder,
	                                               @NotNull ProgressIndicator indicator) throws IOException {

		GitoscAuthData auth = authHolder.getAuthData();

		if (StringUtil.isEmptyOrSpaces(auth.getHost())) {
			throw new GitoscAuthenticationException("Target host not defined");
		}

		try {
			new URI(auth.getHost());
		}
		catch (URISyntaxException e) {
			throw new GitoscAuthenticationException("Invalid host URL");
		}

		switch (auth.getAuthType()) {
			case SESSION:
				GitoscAuthData.SessionAuth sessionAuth = auth.getSessionAuth();
				assert sessionAuth != null;
				if(StringUtil.isEmptyOrSpaces(sessionAuth.getLogin()) || StringUtil.isEmptyOrSpaces(sessionAuth.getPassword())){
					throw new GitoscAuthenticationException("Empty login or password");
				}
				break;
			case BASIC:
				GitoscAuthData.BasicAuth basicAuth = auth.getBasicAuth();
				assert basicAuth != null;
				if (StringUtil.isEmptyOrSpaces(basicAuth.getLogin()) || StringUtil.isEmptyOrSpaces(basicAuth.getPassword())) {
					throw new GitoscAuthenticationException("Empty login or password");
				}
				break;
			case TOKEN:
				GitoscAuthData.TokenAuth tokenAuth = auth.getTokenAuth();
				assert tokenAuth != null;
				if (StringUtil.isEmptyOrSpaces(tokenAuth.getToken())) {
					throw new GitoscAuthenticationException("Empty token");
				}
				break;
			case ANONYMOUS:
				throw new GitoscAuthenticationException("Anonymous connection not allowed");
		}

		return testConnection(project, authHolder, indicator);
	}

	@NotNull
	private static GitoscUserDetailed testConnection(@NotNull Project project,
	                                                 @NotNull GitoscAuthDataHolder authHolder,
	                                                 @NotNull final ProgressIndicator indicator) throws IOException {
		GitoscAuthData auth = authHolder.getAuthData();

		final GitoscConnection connection = new GitoscConnection(auth, true);
		ScheduledFuture<?> future = null;

		try {
			future = addCancellationListener(indicator, connection);
			GitoscUserDetailed userDetailed = GitoscApiUtil.getCurrentUserDetailed(connection, auth);
			auth.setAccessToken(userDetailed.getPrivateToken());
			return userDetailed;
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
	                                                          @NotNull final GitoscConnection connection) {
		return addCancellationListener(() -> {
			if (indicator.isCanceled()) connection.abort();
		});
	}

	@NotNull
	private static ScheduledFuture<?> addCancellationListener(@NotNull final ProgressIndicator indicator,
	                                                          @NotNull final Thread thread) {
		return addCancellationListener(() -> {
			if (indicator.isCanceled()) thread.interrupt();
		});
	}


}
