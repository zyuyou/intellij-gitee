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

import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBCheckBox;
import gnu.trove.Equality;
import org.intellij.gitee.util.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/ui/GithubSettingsPanel.java
 * @author JetBrains s.r.o.
 * @author oleg
 * @date 10/20/10
 */
public class GiteeSettingsPanel {
  private final GiteeSettings mySettings;

  private JPanel myPane;
  private JSpinner myTimeoutSpinner;
  private JBCheckBox myCloneUsingSshCheckBox;
  private GiteeCredentialsPanel myCredentialsPanel;

  public GiteeSettingsPanel() {
    mySettings = GiteeSettings.getInstance();

    reset();
  }

  public JComponent getPanel() {
    return myPane;
  }

  @NotNull
  public GiteeAuthData getAuthData() {
    GiteeAuthData authData = myCredentialsPanel.getAuthData();

    if (authData.getSessionAuth() != null && StringUtil.isEmptyOrSpaces(authData.getSessionAuth().getLogin()) ||
      authData.getTokenAuth() != null && StringUtil.isEmptyOrSpaces(authData.getTokenAuth().getToken())) {

      return GiteeAuthData.createAnonymous(myCredentialsPanel.getHost());
    }

    return authData;
  }

  public void setConnectionTimeout(int timeout) {
    myTimeoutSpinner.setValue(timeout);
  }

  public int getConnectionTimeout() {
    return ((SpinnerNumberModel)myTimeoutSpinner.getModel()).getNumber().intValue();
  }

  public void reset() {
    myCredentialsPanel.setAuthData(mySettings.getAuthData());

    // do not support v3
    myCredentialsPanel.lockAuthType(GiteeAuthData.AuthType.TOKEN);

    setConnectionTimeout(mySettings.getConnectionTimeout());
    myCloneUsingSshCheckBox.setSelected(mySettings.isCloneGitUsingSsh());
  }

  public void apply() {
    if (!equal(mySettings.getAuthData(), getAuthData())) {
      mySettings.setAuthData(getAuthData(), true);
    }
    mySettings.setConnectionTimeout(getConnectionTimeout());
    mySettings.setCloneGitUsingSsh(myCloneUsingSshCheckBox.isSelected());
  }

  public boolean isModified() {
    return !equal(mySettings.getAuthData(), getAuthData()) ||
      !Comparing.equal(mySettings.getConnectionTimeout(), getConnectionTimeout()) ||
      !Comparing.equal(mySettings.isCloneGitUsingSsh(), myCloneUsingSshCheckBox.isSelected());
  }

  private void createUIComponents() {
    myCredentialsPanel = new GiteeCredentialsPanel(ProjectManager.getInstance().getDefaultProject());
    myTimeoutSpinner = new JSpinner(new SpinnerNumberModel(5000, 0, 60000, 500));
  }

  private static boolean equal(@NotNull GiteeAuthData data1, @NotNull GiteeAuthData data2) {
    return Comparing.equal(data1.getHost(), data2.getHost())
      && Comparing.equal(data1.getAuthType(), data2.getAuthType())
      && equal(data1.getSessionAuth(), data2.getSessionAuth(), (auth1, auth2) -> Comparing.equal(auth1.getLogin(), auth2.getLogin()) && Comparing.equal(auth1.getPassword(), auth2.getPassword()))
      && equal(data1.getBasicAuth(), data2.getBasicAuth(), (auth1, auth2) -> Comparing.equal(auth1.getLogin(), auth2.getLogin()) && Comparing.equal(auth1.getPassword(), auth2.getPassword()))
      && equal(data1.getTokenAuth(), data2.getTokenAuth(), (auth1, auth2) -> Comparing.equal(auth1.getToken(), auth2.getToken()));
  }

  private static <T> boolean equal(@Nullable T o1, @Nullable T o2, @NotNull Equality<T> notNullEquality) {
    if (o1 == o2) return true;
    if (o1 == null) return false;
    if (o2 == null) return false;
    return notNullEquality.equals(o1, o2);
  }
}
