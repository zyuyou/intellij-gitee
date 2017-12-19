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

package org.intellij.gitosc.tasks;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.tasks.config.BaseRepositoryEditor;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.Consumer;
import com.intellij.util.ui.FormBuilder;
import com.intellij.util.ui.GridBag;
import org.intellij.gitosc.GitoscConstants;
import org.intellij.gitosc.api.GitoscApiUtil;
import org.intellij.gitosc.util.AuthLevel;
import org.intellij.gitosc.util.GitoscAuthDataHolder;
import org.intellij.gitosc.util.GitoscNotifications;
import org.intellij.gitosc.util.GitoscUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.IOException;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/tasks/GithubRepositoryEditor.java
 * @author JetBrains s.r.o.
 * @author Dennis.Ushakov
 */
public class GitoscRepositoryEditor extends BaseRepositoryEditor<GitoscRepository> {
  private MyTextField myRepoAuthor;
  private MyTextField myRepoName;
  private MyTextField myToken;
  private JBCheckBox myShowNotAssignedIssues;
  private JButton myTokenButton;
  private JBLabel myHostLabel;
  private JBLabel myRepositoryLabel;
  private JBLabel myTokenLabel;

  public GitoscRepositoryEditor(final Project project, final GitoscRepository repository, Consumer<GitoscRepository> changeListener) {
    super(project, repository, changeListener);
    myUrlLabel.setVisible(false);
    myUsernameLabel.setVisible(false);
    myUserNameText.setVisible(false);
    myPasswordLabel.setVisible(false);
    myPasswordText.setVisible(false);
    myUseHttpAuthenticationCheckBox.setVisible(false);

    myRepoAuthor.setText(repository.getRepoAuthor());
    myRepoName.setText(repository.getRepoName());
    myToken.setText(repository.getToken());
    myToken.setText(repository.getToken());
    myShowNotAssignedIssues.setSelected(!repository.isAssignedIssuesOnly());

    DocumentListener buttonUpdater = new DocumentAdapter() {
      @Override
      protected void textChanged(DocumentEvent e) {
        updateTokenButton();
      }
    };

    myURLText.getDocument().addDocumentListener(buttonUpdater);
    myRepoAuthor.getDocument().addDocumentListener(buttonUpdater);
    myRepoName.getDocument().addDocumentListener(buttonUpdater);
  }

  @Nullable
  @Override
  protected JComponent createCustomPanel() {
    myHostLabel = new JBLabel("Host:", SwingConstants.RIGHT);

    JPanel myHostPanel = new JPanel(new BorderLayout(5, 0));
    myHostPanel.add(myURLText, BorderLayout.CENTER);
    myHostPanel.add(myShareUrlCheckBox, BorderLayout.EAST);

    myRepositoryLabel = new JBLabel("Repository:", SwingConstants.RIGHT);
    myRepoAuthor = new MyTextField("Repository Owner");
    myRepoName = new MyTextField("Repository Name");
    myRepoAuthor.setPreferredSize("SomelongNickname");
    myRepoName.setPreferredSize("SomelongReponame-with-suffixes");

    JPanel myRepoPanel = new JPanel(new GridBagLayout());
    GridBag bag = new GridBag().setDefaultWeightX(1).setDefaultFill(GridBagConstraints.HORIZONTAL);
    myRepoPanel.add(myRepoAuthor, bag.nextLine().next());
    myRepoPanel.add(new JLabel("/"), bag.next().fillCellNone().insets(0, 5, 0, 5).weightx(0));
    myRepoPanel.add(myRepoName, bag.next());

    myTokenLabel = new JBLabel("API Token:", SwingConstants.RIGHT);
    myToken = new MyTextField("OAuth2 token");
    myTokenButton = new JButton("Create API token");
    myTokenButton.addActionListener(e -> {
      generateToken();
      doApply();
    });

    JPanel myTokenPanel = new JPanel();
    myTokenPanel.setLayout(new BorderLayout(5, 5));
    myTokenPanel.add(myToken, BorderLayout.CENTER);
    myTokenPanel.add(myTokenButton, BorderLayout.EAST);

    myShowNotAssignedIssues = new JBCheckBox("Include issues not assigned to me");

    installListener(myRepoAuthor);
    installListener(myRepoName);
    installListener(myToken);
    installListener(myShowNotAssignedIssues);

    return FormBuilder.createFormBuilder()
      .setAlignLabelOnRight(true)
      .addLabeledComponent(myHostLabel, myHostPanel)
      .addLabeledComponent(myRepositoryLabel, myRepoPanel)
      .addLabeledComponent(myTokenLabel, myTokenPanel)
      .addComponentToRightColumn(myShowNotAssignedIssues)
      .getPanel();
  }

  @Override
  public void apply() {
    myRepository.setRepoName(getRepoName());
    myRepository.setRepoAuthor(getRepoAuthor());
    myRepository.setToken(getToken());
    myRepository.setAssignedIssuesOnly(isAssignedIssuesOnly());
    super.apply();
  }

  private void generateToken() {
    try {
      String token = GitoscUtil.computeValueInModalIO(myProject, GitoscConstants.TITLE_ACCESS_TO_GITOSC, indicator ->
        GitoscUtil.runTask(myProject, GitoscAuthDataHolder.createFromSettings(), indicator, AuthLevel.basicOnetime(getHost()), connection ->
          GitoscApiUtil.getTasksToken(connection, getRepoAuthor(), getRepoName(), "IntelliJ tasks plugin")
        ));
      myToken.setText(token);
    }
    catch (IOException e) {
      GitoscNotifications.showErrorDialog(myProject, "Can't Get Access Token", e);
    }
  }

  @Override
  public void setAnchor(@Nullable final JComponent anchor) {
    super.setAnchor(anchor);
    myHostLabel.setAnchor(anchor);
    myRepositoryLabel.setAnchor(anchor);
    myTokenLabel.setAnchor(anchor);
  }

  private void updateTokenButton() {
    if (StringUtil.isEmptyOrSpaces(getHost()) ||
        StringUtil.isEmptyOrSpaces(getRepoAuthor()) ||
        StringUtil.isEmptyOrSpaces(getRepoName())) {
      myTokenButton.setEnabled(false);
    }else {
      myTokenButton.setEnabled(true);
    }
  }

  @NotNull
  private String getHost() {
    return myURLText.getText().trim();
  }

  @NotNull
  private String getRepoAuthor() {
    return myRepoAuthor.getText().trim();
  }

  @NotNull
  private String getRepoName() {
    return myRepoName.getText().trim();
  }

  @NotNull
  private String getToken() {
    return myToken.getText().trim();
  }

  private boolean isAssignedIssuesOnly() {
    return !myShowNotAssignedIssues.isSelected();
  }

  public static class MyTextField extends JBTextField {
    private int myWidth = -1;

    public MyTextField(@NotNull String hintCaption) {
      getEmptyText().setText(hintCaption);
    }

    public void setPreferredSize(@NotNull String sampleSizeString) {
      myWidth = getFontMetrics(getFont()).stringWidth(sampleSizeString);
    }

    @Override
    public Dimension getPreferredSize() {
      Dimension size = super.getPreferredSize();
      if (myWidth != -1) {
        size.width = myWidth;
      }
      return size;
    }
  }
}