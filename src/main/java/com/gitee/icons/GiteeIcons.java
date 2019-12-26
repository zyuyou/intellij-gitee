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
package com.gitee.icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

/**
 * @author Yuyou Chow
 */
public final class GiteeIcons {
  private static Icon load(String path) {
    return IconLoader.getIcon(path, GiteeIcons.class);
  }

  public static final Icon DefaultAvatar = load("/icons/defaultAvatar.svg"); // 16x16
  public static final Icon PullRequestClosed = load("/icons/pullRequestClosed.svg"); // 16x16
  public static final Icon PullRequestMerged = load("/icons/pullRequestMerged.svg"); // 16x16
  public static final Icon PullRequestOpen = load("/icons/pullRequestOpen.svg"); // 16x16
  public static final Icon PullRequestsToolWindow = load("/icons/pullRequestsToolWindow.svg"); // 13x13
  public static final Icon Review = load("/icons/review.svg"); // 16x16
  public static final Icon ReviewAccepted = load("/icons/reviewAccepted.svg"); // 16x16
  public static final Icon ReviewRejected = load("/icons/reviewRejected.svg"); // 16x16
  public static final Icon Timeline = load("/icons/timeline.svg"); // 16x16

  public static final Icon Gitee_icon = load("/icons/gitee.svg");

}
