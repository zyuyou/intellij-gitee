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
package org.intellij.gitee.ui;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import org.intellij.gitee.GiteeBundle;
import org.intellij.gitee.util.GiteeSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/ui/GithubCreateGistDialog.java
 * @author JetBrains s.r.o.
 * @author oleg
 */
public class GiteeCreateGistDialog extends DialogWrapper {
  private final GiteeCreateGistPanel myGiteeCreateGistPanel;

  public GiteeCreateGistDialog(@NotNull final Project project, @Nullable Editor editor, @Nullable VirtualFile[] files, @Nullable VirtualFile file) {
    super(project, true);
    myGiteeCreateGistPanel = new GiteeCreateGistPanel();
    // Use saved settings for controls
    final GiteeSettings settings = GiteeSettings.getInstance();
    myGiteeCreateGistPanel.setAnonymous(settings.isAnonymousGist());
    myGiteeCreateGistPanel.setPrivate(settings.isPrivateGist());
    myGiteeCreateGistPanel.setOpenInBrowser(settings.isOpenInBrowserGist());

    if (editor != null) {
      if (file != null) {
        myGiteeCreateGistPanel.showFileNameField(file.getName());
      }
      else {
        myGiteeCreateGistPanel.showFileNameField("");
      }
    }
    else if (files != null) {
      if (files.length == 1 && !files[0].isDirectory()) {
        myGiteeCreateGistPanel.showFileNameField(files[0].getName());
      }
    }
    else if (file != null && !file.isDirectory()) {
      myGiteeCreateGistPanel.showFileNameField(file.getName());
    }

    setTitle(GiteeBundle.message2("gitee.create.gist.title"));
    init();
  }

  @Override
  protected JComponent createCenterPanel() {
    return myGiteeCreateGistPanel.getPanel();
  }

  @Override
  protected String getHelpId() {
    return "gitee.create.gist.dialog";
  }

  @Override
  protected String getDimensionServiceKey() {
    return "Gitee.CreateGistDialog";
  }

  @Override
  protected void doOKAction() {
    // Store settings
    final GiteeSettings settings = GiteeSettings.getInstance();
    settings.setAnonymousGist(myGiteeCreateGistPanel.isAnonymous());
    settings.setOpenInBrowserGist(myGiteeCreateGistPanel.isOpenInBrowser());
    settings.setPrivateGist(myGiteeCreateGistPanel.isPrivate());
    super.doOKAction();
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myGiteeCreateGistPanel.getDescriptionTextArea();
  }

  public boolean isPrivate() {
    return myGiteeCreateGistPanel.isPrivate();
  }

  public boolean isAnonymous() {
    return myGiteeCreateGistPanel.isAnonymous();
  }

  @NotNull
  public String getDescription() {
    return myGiteeCreateGistPanel.getDescriptionTextArea().getText();
  }

  @Nullable
  public String getFileName() {
    return myGiteeCreateGistPanel.getFileNameField().getText();
  }

  public boolean isOpenInBrowser() {
    return myGiteeCreateGistPanel.isOpenInBrowser();
  }
}
