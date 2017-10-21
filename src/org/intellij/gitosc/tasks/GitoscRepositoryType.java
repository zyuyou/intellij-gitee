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
import com.intellij.tasks.TaskRepository;
import com.intellij.tasks.TaskState;
import com.intellij.tasks.config.TaskRepositoryEditor;
import com.intellij.tasks.impl.BaseRepositoryType;
import com.intellij.util.Consumer;
import icons.TasksCoreIcons;
import org.intellij.gitosc.GitoscBundle;
import org.intellij.gitosc.GitoscConstants;
import org.intellij.gitosc.icons.GitoscIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.EnumSet;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/tasks/GithubRepositoryType.java
 * @author JetBrains s.r.o.
 * @author Dennis.Ushakov
 */
public class GitoscRepositoryType extends BaseRepositoryType<GitoscRepository> {

  @NotNull
  @Override
  public String getName() {
    return "GitOSC";
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return GitoscIcons.GITOSC_SMALL;
  }

  @NotNull
  @Override
  public TaskRepository createRepository() {
    return new GitoscRepository(this);
  }

  @Override
  public Class<GitoscRepository> getRepositoryClass() {
    return GitoscRepository.class;
  }

  @NotNull
  @Override
  public TaskRepositoryEditor createEditor(GitoscRepository repository,
                                           Project project,
                                           Consumer<GitoscRepository> changeListener) {
    return new GitoscRepositoryEditor(project, repository, changeListener);
  }

  public EnumSet<TaskState> getPossibleTaskStates() {
    return EnumSet.of(TaskState.OPEN, TaskState.RESOLVED);
  }

}
