/*
 * Copyright 2016 码云
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
package org.intellij.gitosc.actions;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import git4idea.GitUtil;
import git4idea.repo.GitRepository;
import org.intellij.gitosc.GitoscConstants;
import org.intellij.gitosc.api.GitoscFullPath;
import org.intellij.gitosc.icons.GitoscIcons;
import org.intellij.gitosc.util.GitoscNotifications;
import org.intellij.gitosc.util.GitoscUrlUtil;
import org.intellij.gitosc.util.GitoscUtil;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/GithubShowCommitInBrowserAction.java
 * @author JetBrains s.r.o.
 * @author Kirill Likhodedov
 */
abstract class GitoscShowCommitInBrowserAction extends DumbAwareAction {

  public GitoscShowCommitInBrowserAction() {
    super("Open on GitOSC", "Open the selected commit in browser", GitoscIcons.GITOSC_SMALL);
  }

  protected static void openInBrowser(Project project, GitRepository repository, String revisionHash) {
    String url = GitoscUtil.findGitoscRemoteUrl(repository);
    if (url == null) {
      GitoscConstants.LOG.info(String.format("Repository is not under GitOSC. Root: %s, Remotes: %s", repository.getRoot(),
                                           GitUtil.getPrintableRemotes(repository.getRemotes())));
      return;
    }
    GitoscFullPath userAndRepository = GitoscUrlUtil.getUserAndRepositoryFromRemoteUrl(url);
    if (userAndRepository == null) {
      GitoscNotifications
        .showError(project, GitoscOpenInBrowserAction.CANNOT_OPEN_IN_BROWSER, "Cannot extract info about repository: " + url);
      return;
    }

    String gitoscUrl = GitoscUrlUtil.getGitoscHost() + '/' + userAndRepository.getUser() + '/'
                       + userAndRepository.getRepository() + "/commit/" + revisionHash;
    BrowserUtil.browse(gitoscUrl);
  }

}
