/*
 * Copyright 2016-2018 码云 - Gitee
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
package com.gitee.util;

import com.gitee.exceptions.GiteeOperationCanceledException;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.VcsNotifier;
import org.apache.http.client.ClientProtocolException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.net.UnknownHostException;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/util/GithubNotifications.java
 * @author JetBrains s.r.o.
 */
public class GiteeNotifications {
	private static final Logger LOG = GiteeUtil.LOG;

	private static boolean isOperationCanceled(@NotNull Throwable e) {
		return e instanceof GiteeOperationCanceledException ||
			e instanceof ProcessCanceledException;
	}

	@NotNull
	public static String getErrorTextFromException(@NotNull Exception e) {
		if (e instanceof UnknownHostException) {
			return "Unknown host: " + e.getMessage();
		}else if(e instanceof ClientProtocolException){
			return e.getCause().getMessage();
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

	public static void showWarning(@NotNull Project project, @NotNull String title, @NotNull String message, @Nullable AnAction... actions) {
		LOG.info(title + "; " + message);

		Notification notification = new Notification(VcsNotifier.IMPORTANT_ERROR_NOTIFICATION.getDisplayId(), title, message, NotificationType.WARNING);

		if (actions != null) {
			for (AnAction action : actions) {
				notification.addAction(action);
			}
		}
		notification.notify(project);
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

	public static void showError(@NotNull Project project, @NotNull String title, @NotNull Throwable e) {
		LOG.warn(title + "; ", e);
		if (isOperationCanceled(e)) return;
		VcsNotifier.getInstance(project).notifyError(title, GiteeUtil.getErrorTextFromException(e));
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

	@NotNull
	public static AnAction getConfigureAction(@NotNull Project project) {
		return NotificationAction.createSimple("Configure...", () -> {
			ShowSettingsUtil.getInstance().showSettingsDialog(project, GiteeUtil.SERVICE_DISPLAY_NAME);
		});
	}

}
