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
package org.intellij.gitosc.ui;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.vfs.VirtualFile;
import org.intellij.gitosc.GitoscBundle;
import org.intellij.gitosc.util.GitoscSettings;
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
public class GitoscCreateGistDialog extends DialogWrapper {
  private final GitoscCreateGistPanel myGitoscCreateGistPanel;

  public GitoscCreateGistDialog(@NotNull final Project project, @Nullable Editor editor, @Nullable VirtualFile[] files, @Nullable VirtualFile file) {
    super(project, true);
    myGitoscCreateGistPanel = new GitoscCreateGistPanel();
    // Use saved settings for controls
    final GitoscSettings settings = GitoscSettings.getInstance();
    myGitoscCreateGistPanel.setAnonymous(settings.isAnonymousGist());
    myGitoscCreateGistPanel.setPrivate(settings.isPrivateGist());
    myGitoscCreateGistPanel.setOpenInBrowser(settings.isOpenInBrowserGist());

    if (editor != null) {
      if (file != null) {
        myGitoscCreateGistPanel.showFileNameField(file.getName());
      }
      else {
        myGitoscCreateGistPanel.showFileNameField("");
      }
    }
    else if (files != null) {
      if (files.length == 1 && !files[0].isDirectory()) {
        myGitoscCreateGistPanel.showFileNameField(files[0].getName());
      }
    }
    else if (file != null && !file.isDirectory()) {
      myGitoscCreateGistPanel.showFileNameField(file.getName());
    }

    setTitle(GitoscBundle.message2("gitosc.create.gist.title"));
    init();
  }

  @Override
  protected JComponent createCenterPanel() {
    return myGitoscCreateGistPanel.getPanel();
  }

  @Override
  protected String getHelpId() {
    return "gitosc.create.gist.dialog";
  }

  @Override
  protected String getDimensionServiceKey() {
    return "Gitosc.CreateGistDialog";
  }

  @Override
  protected void doOKAction() {
    // Store settings
    final GitoscSettings settings = GitoscSettings.getInstance();
    settings.setAnonymousGist(myGitoscCreateGistPanel.isAnonymous());
    settings.setOpenInBrowserGist(myGitoscCreateGistPanel.isOpenInBrowser());
    settings.setPrivateGist(myGitoscCreateGistPanel.isPrivate());
    super.doOKAction();
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myGitoscCreateGistPanel.getDescriptionTextArea();
  }

  public boolean isPrivate() {
    return myGitoscCreateGistPanel.isPrivate();
  }

  public boolean isAnonymous() {
    return myGitoscCreateGistPanel.isAnonymous();
  }

  @NotNull
  public String getDescription() {
    return myGitoscCreateGistPanel.getDescriptionTextArea().getText();
  }

  @Nullable
  public String getFileName() {
    return myGitoscCreateGistPanel.getFileNameField().getText();
  }

  public boolean isOpenInBrowser() {
    return myGitoscCreateGistPanel.isOpenInBrowser();
  }
}
