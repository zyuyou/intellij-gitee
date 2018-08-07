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
package com.gitee.tasks;

import com.gitee.icons.GiteeIcons;
import com.intellij.openapi.project.Project;
import com.intellij.tasks.TaskRepository;
import com.intellij.tasks.TaskState;
import com.intellij.tasks.config.TaskRepositoryEditor;
import com.intellij.tasks.impl.BaseRepositoryType;
import com.intellij.util.Consumer;
import com.gitee.icons.GiteeIcons;
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
public class GiteeRepositoryType extends BaseRepositoryType<GiteeRepository> {

  @NotNull
  @Override
  public String getName() {
    return "Gitee";
  }

  @NotNull
  @Override
  public Icon getIcon() {
    return GiteeIcons.Gitee_icon;
  }

  @NotNull
  @Override
  public TaskRepository createRepository() {
    return new GiteeRepository(this);
  }

  @Override
  public Class<GiteeRepository> getRepositoryClass() {
    return GiteeRepository.class;
  }

  @NotNull
  @Override
  public TaskRepositoryEditor createEditor(GiteeRepository repository,
                                           Project project,
                                           Consumer<GiteeRepository> changeListener) {
    return new GiteeRepositoryEditor(project, repository, changeListener);
  }

  public EnumSet<TaskState> getPossibleTaskStates() {
    return EnumSet.of(TaskState.OPEN, TaskState.RESOLVED);
  }

}
