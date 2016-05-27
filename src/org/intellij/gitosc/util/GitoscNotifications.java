package org.intellij.gitosc.util;

import com.intellij.notification.NotificationListener;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsNotifier;
import org.intellij.gitosc.exceptions.GitoscOperationCanceledException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.net.UnknownHostException;

/**
 * Created by zyuyou on 16/5/25.
 *
 * https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/util/GithubNotifications.java
 */
public class GitoscNotifications {
	private static final Logger LOG = GitoscUtil.LOG;

	private static boolean isOperationCanceled(@NotNull Exception e) {
		return e instanceof GitoscOperationCanceledException ||
			e instanceof ProcessCanceledException;
	}

	@NotNull
	public static String getErrorTextFromException(@NotNull Exception e) {
		if (e instanceof UnknownHostException) {
			return "Unknown host: " + e.getMessage();
		}
		return e.getMessage();
	}

	//====================================================================================
	// - start - Show msg functions
	//====================================================================================
	public static void showInfo(@NotNull Project project, @NotNull String title, @NotNull String message) {
		LOG.info(title + "; " + message);
		VcsNotifier.getInstance(project).notifyImportantInfo(title, message);
	}

	public static void showWarning(@NotNull Project project, @NotNull String title, @NotNull String message) {
		LOG.info(title + "; " + message);
		VcsNotifier.getInstance(project).notifyImportantWarning(title, message);
	}

	public static void showWarning(@NotNull Project project, @NotNull String title, @NotNull Exception e) {
		LOG.info(title + "; ", e);
		if (isOperationCanceled(e)) return;
		VcsNotifier.getInstance(project).notifyImportantWarning(title, getErrorTextFromException(e));
	}

	public static void showError(@NotNull Project project, @NotNull String title, @NotNull String message) {
		LOG.info(title + "; " + message);
		VcsNotifier.getInstance(project).notifyError(title, message);
	}

	public static void showError(@NotNull Project project, @NotNull String title, @NotNull String message, @NotNull String logDetails) {
		LOG.warn(title + "; " + message + "; " + logDetails);
		VcsNotifier.getInstance(project).notifyError(title, message);
	}

	public static void showError(@NotNull Project project, @NotNull String title, @NotNull Exception e) {
		LOG.warn(title + "; ", e);
		if (isOperationCanceled(e)) return;
		VcsNotifier.getInstance(project).notifyError(title, getErrorTextFromException(e));
	}
	//====================================================================================
	// - end - Show msg functions
	//====================================================================================

	//====================================================================================
	// - start - Show url functions
	//====================================================================================
	public static void showInfoURL(@NotNull Project project, @NotNull String title, @NotNull String message, @NotNull String url) {
		LOG.info(title + "; " + message + "; " + url);
		VcsNotifier.getInstance(project)
			.notifyImportantInfo(title, "<a href='" + url + "'>" + message + "</a>", NotificationListener.URL_OPENING_LISTENER);
	}

	public static void showWarningURL(@NotNull Project project,
	                                  @NotNull String title,
	                                  @NotNull String prefix,
	                                  @NotNull String highlight,
	                                  @NotNull String postfix,
	                                  @NotNull String url) {
		LOG.info(title + "; " + prefix + highlight + postfix + "; " + url);
		VcsNotifier.getInstance(project).notifyImportantWarning(title, prefix + "<a href='" + url + "'>" + highlight + "</a>" + postfix,
			NotificationListener.URL_OPENING_LISTENER);
	}

	public static void showErrorURL(@NotNull Project project,
	                                @NotNull String title,
	                                @NotNull String prefix,
	                                @NotNull String highlight,
	                                @NotNull String postfix,
	                                @NotNull String url) {
		LOG.info(title + "; " + prefix + highlight + postfix + "; " + url);
		VcsNotifier.getInstance(project).notifyError(title, prefix + "<a href='" + url + "'>" + highlight + "</a>" + postfix,
			NotificationListener.URL_OPENING_LISTENER);
	}
	//====================================================================================
	// - end - Show url functions
	//====================================================================================

	//====================================================================================
	// - start - Show dialog functions
	//====================================================================================
	public static void showInfoDialog(@Nullable Project project, @NotNull String title, @NotNull String message) {
		LOG.info(title + "; " + message);
		Messages.showInfoMessage(project, message, title);
	}

	public static void showInfoDialog(@NotNull Component component, @NotNull String title, @NotNull String message) {
		LOG.info(title + "; " + message);
		Messages.showInfoMessage(component, message, title);
	}

	public static void showWarningDialog(@Nullable Project project, @NotNull String title, @NotNull String message) {
		LOG.info(title + "; " + message);
		Messages.showWarningDialog(project, message, title);
	}

	public static void showWarningDialog(@NotNull Component component, @NotNull String title, @NotNull String message) {
		LOG.info(title + "; " + message);
		Messages.showWarningDialog(component, message, title);
	}

	public static void showErrorDialog(@Nullable Project project, @NotNull String title, @NotNull String message) {
		LOG.info(title + "; " + message);
		Messages.showErrorDialog(project, message, title);
	}

	public static void showErrorDialog(@Nullable Project project, @NotNull String title, @NotNull Exception e) {
		LOG.warn(title, e);
		if (isOperationCanceled(e)) return;
		Messages.showErrorDialog(project, getErrorTextFromException(e), title);
	}

	public static void showErrorDialog(@NotNull Component component, @NotNull String title, @NotNull Exception e) {
		LOG.info(title, e);
		if (isOperationCanceled(e)) return;
		Messages.showErrorDialog(component, getErrorTextFromException(e), title);
	}

	public static void showErrorDialog(@NotNull Component component, @NotNull String title, @NotNull String prefix, @NotNull Exception e) {
		LOG.info(title, e);
		if (isOperationCanceled(e)) return;
		Messages.showErrorDialog(component, prefix + getErrorTextFromException(e), title);
	}

	@Messages.YesNoResult
	public static boolean showYesNoDialog(@Nullable Project project, @NotNull String title, @NotNull String message) {
		return Messages.YES == Messages.showYesNoDialog(project, message, title, Messages.getQuestionIcon());
	}

	@Messages.YesNoResult
	public static boolean showYesNoDialog(@Nullable Project project,
	                                      @NotNull String title,
	                                      @NotNull String message,
	                                      @NotNull DialogWrapper.DoNotAskOption doNotAskOption) {
		return Messages.YES == Messages.showYesNoDialog(project, message, title, Messages.getQuestionIcon(), doNotAskOption);
	}
	//====================================================================================
	// - end - Show dialog functions
	//====================================================================================

}
