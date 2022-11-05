/*
 *  Copyright 2016-2019 码云 - Gitee
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.gitee.util;

import com.gitee.exceptions.GiteeOperationCanceledException;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationListener;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.DoNotAskOption;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.HtmlChunk;
import com.intellij.openapi.vcs.VcsNotifier;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.gitee.util.GiteeUtil.getErrorTextFromException;
import static com.intellij.openapi.util.NlsContexts.NotificationContent;
import static com.intellij.openapi.util.NlsContexts.NotificationTitle;

/**
 * @author Yuyou Chow
 * <p>
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/util/GithubNotifications.java
 * @author JetBrains s.r.o.
 */
public class GiteeNotifications {
  private static final Logger LOG = GiteeUtil.LOG;

  private static boolean isOperationCanceled(@NotNull Throwable e) {
    return e instanceof GiteeOperationCanceledException ||
        e instanceof ProcessCanceledException;
  }

  //====================================================================================
  // - start - Show msg functions
  //====================================================================================
  public static void showInfo(@NotNull Project project,
                              @NonNls @Nullable String displayId,
                              @NotificationTitle @NotNull String title,
                              @NotificationContent @NotNull String message) {
    LOG.info(title + "; " + message);
    VcsNotifier.getInstance(project).notifyImportantInfo(displayId, title, message);
  }

  public static void showWarning(@NotNull Project project,
                                 @NonNls @Nullable String displayId,
                                 @NotificationTitle @NotNull String title,
                                 @NotificationContent @NotNull String message) {
    LOG.info(title + "; " + message);
    VcsNotifier.getInstance(project).notifyImportantWarning(displayId, title, message);
  }

  public static void showError(@NotNull Project project,
                               @NonNls @Nullable String displayId,
                               @NotificationTitle @NotNull String title,
                               @NotificationContent @NotNull String message) {
    LOG.info(title + "; " + message);
    VcsNotifier.getInstance(project).notifyError(displayId, title, message);
  }

  public static void showError(@NotNull Project project,
                               @NonNls @Nullable String displayId,
                               @NotificationTitle @NotNull String title,
                               @NotificationContent @NotNull String message,
                               @NotNull String logDetails) {
    LOG.warn(title + "; " + message + "; " + logDetails);
    VcsNotifier.getInstance(project).notifyError(displayId, title, message);
  }

  public static void showError(@NotNull Project project,
                               @NonNls @Nullable String displayId,
                               @NotificationTitle @NotNull String title,
                               @NotNull Throwable e) {
    LOG.warn(title + "; ", e);
    if (isOperationCanceled(e)) return;
    VcsNotifier.getInstance(project).notifyError(displayId, title, getErrorTextFromException(e));
  }

  //====================================================================================
  // - end - Show msg functions
  //====================================================================================

  //====================================================================================
  // - start - Show url functions
  //====================================================================================
  public static void showInfoURL(@NotNull Project project,
                                 @NonNls @Nullable String displayId,
                                 @NotificationTitle @NotNull String title,
                                 @NotificationContent @NotNull String message,
                                 @NotNull String url) {
    LOG.info(title + "; " + message + "; " + url);
    VcsNotifier.getInstance(project)
        .notifyImportantInfo(displayId, title, HtmlChunk.link(url, message).toString(), NotificationListener.URL_OPENING_LISTENER);
  }

  public static void showWarningURL(@NotNull Project project,
                                    @NonNls @Nullable String displayId,
                                    @NotificationTitle @NotNull String title,
                                    @NotNull String prefix,
                                    @NotNull String highlight,
                                    @NotNull String postfix,
                                    @NotNull String url) {
    LOG.info(title + "; " + prefix + highlight + postfix + "; " + url);
    //noinspection HardCodedStringLiteral
    VcsNotifier.getInstance(project).notifyImportantWarning(
        displayId,
        title,
        prefix + "<a href='" + url + "'>" + highlight + "</a>" + postfix,
        NotificationListener.URL_OPENING_LISTENER
    );
  }

  public static void showErrorURL(@NotNull Project project,
                                  @NonNls @Nullable String displayId,
                                  @NotificationTitle @NotNull String title,
                                  @NotNull String prefix,
                                  @NotNull String highlight,
                                  @NotNull String postfix,
                                  @NotNull String url) {
    LOG.info(title + "; " + prefix + highlight + postfix + "; " + url);
    //noinspection HardCodedStringLiteral
    VcsNotifier.getInstance(project).notifyError(
        displayId,
        title,
        prefix + "<a href='" + url + "'>" + highlight + "</a>" + postfix,
        NotificationListener.URL_OPENING_LISTENER
    );
  }
  //====================================================================================
  // - end - Show url functions
  //====================================================================================

  //====================================================================================
  // - start - Show dialog functions
  //====================================================================================
  public static void showWarningDialog(@Nullable Project project,
                                       @NotificationTitle @NotNull String title,
                                       @NotificationContent @NotNull String message) {
    LOG.info(title + "; " + message);
    Messages.showWarningDialog(project, message, title);
  }

  public static void showErrorDialog(@Nullable Project project,
                                     @NotificationTitle @NotNull String title,
                                     @NotificationContent @NotNull String message) {
    LOG.info(title + "; " + message);
    Messages.showErrorDialog(project, message, title);
  }

  @Messages.YesNoResult
  public static boolean showYesNoDialog(@Nullable Project project,
                                        @NotificationTitle @NotNull String title,
                                        @NotificationContent @NotNull String message) {

    return MessageDialogBuilder.yesNo(title, message).ask(project);
  }

  @Messages.YesNoResult
  public static boolean showYesNoDialog(@Nullable Project project,
                                        @NotificationTitle @NotNull String title,
                                        @NotificationContent @NotNull String message,
                                        @NotNull DoNotAskOption doNotAskOption) {

    return MessageDialogBuilder.yesNo(title, message)
        .icon(Messages.getQuestionIcon())
        .doNotAsk(doNotAskOption)
        .ask(project);
  }
  //====================================================================================
  // - end - Show dialog functions
  //====================================================================================

  @NotNull
  public static AnAction getConfigureAction(@NotNull Project project) {
    return NotificationAction.createSimple(
        "Configure...",
        () -> ShowSettingsUtil.getInstance().showSettingsDialog(project, GiteeUtil.SERVICE_DISPLAY_NAME)
    );
  }

}
