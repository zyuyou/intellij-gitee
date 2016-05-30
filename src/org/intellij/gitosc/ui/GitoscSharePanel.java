/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.intellij.gitosc.ui;

import com.intellij.ui.DocumentAdapter;

import javax.swing.*;
import javax.swing.event.DocumentEvent;

/**
 * https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/ui/GithubSharePanel.java
 */
public class GitoscSharePanel {
  private JPanel myPanel;
  private JTextField myRepositoryTextField;
  private JCheckBox myPrivateCheckBox;
  private JTextArea myDescriptionTextArea;
  private JTextField myRemoteTextField;
  private final GitoscShareDialog myGitoscShareDialog;

  public GitoscSharePanel(final GitoscShareDialog gitoscShareDialog) {
    myGitoscShareDialog = gitoscShareDialog;
    myPrivateCheckBox.setSelected(false);

    DocumentAdapter changeListener = new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        myGitoscShareDialog.updateOkButton();
      }
    };
    myRepositoryTextField.getDocument().addDocumentListener(changeListener);
    myRemoteTextField.getDocument().addDocumentListener(changeListener);
  }

  public JPanel getPanel() {
    return myPanel;
  }

  public JComponent getPreferredFocusComponent() {
    return myRepositoryTextField;
  }

  public String getRepositoryName() {
    return myRepositoryTextField.getText().trim();
  }

  public void setRepositoryName(final String name) {
    myRepositoryTextField.setText(name);
  }

  public String getRemoteName() {
    return myRemoteTextField.getText().trim();
  }

  public void setRemoteName(final String name) {
    myRemoteTextField.setText(name);
  }

  public boolean isPrivate() {
    return myPrivateCheckBox.isSelected();
  }

  public String getDescription() {
    return myDescriptionTextArea.getText().trim();
  }

  public void setPrivateRepoAvailable(final boolean privateRepoAllowed) {
    if (!privateRepoAllowed) {
      myPrivateCheckBox.setEnabled(false);
      myPrivateCheckBox.setToolTipText("Your account doesn't support private repositories");
    }
  }
}
