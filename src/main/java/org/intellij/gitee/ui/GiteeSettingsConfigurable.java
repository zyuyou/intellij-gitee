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

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vcs.VcsConfigurableProvider;
import org.intellij.gitee.GiteeBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/ui/GithubSettingsConfigurable.java
 * @author JetBrains s.r.o.
 * @author oleg
 */
public class GiteeSettingsConfigurable implements SearchableConfigurable, VcsConfigurableProvider {
  private GiteeSettingsPanel mySettingsPane;

  public GiteeSettingsConfigurable() {
  }

  @NotNull
  public String getDisplayName() {
    return GiteeBundle.message2("gitee.name");
  }

  @NotNull
  public String getHelpTopic() {
    return "settings.gitee";
  }

  @NotNull
  public JComponent createComponent() {
    if (mySettingsPane == null) {
      mySettingsPane = new GiteeSettingsPanel();
    }
    return mySettingsPane.getPanel();
  }

  public boolean isModified() {
    return mySettingsPane != null && mySettingsPane.isModified();
  }

  public void apply() throws ConfigurationException {
    if (mySettingsPane != null) {
      mySettingsPane.apply();
    }
  }

  public void reset() {
    if (mySettingsPane != null) {
      mySettingsPane.reset();
    }
  }

  public void disposeUIResources() {
    mySettingsPane = null;
  }

  @NotNull
  public String getId() {
    return getHelpTopic();
  }

  public Runnable enableSearch(String option) {
    return null;
  }

  @Nullable
  @Override
  public Configurable getConfigurable(Project project) {
    return this;
  }
}
