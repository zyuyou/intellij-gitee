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
package com.gitee.api.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.io.mandatory.RestModel;

@RestModel
@SuppressWarnings("UnusedDeclaration")
public class GiteePullRequestDetailed extends GiteePullRequest {
  private Boolean merged;
  private Boolean mergeable;
  private Boolean rebaseable;
  private String mergeableState;
  private GiteeUser mergedBy;

  private Integer comments;
  private Integer reviewComments;
  private Boolean maintainerCanModify;
  private Integer commits;
  private Integer additions;
  private Integer deletions;
  private Integer changedFiles;

  private String bodyHtml;

  private Tag head;
  private Tag base;

  public boolean getMerged() {
    return merged;
  }

  public boolean getMergeable() {
    return mergeable != null && mergeable;
  }

  public boolean getRebaseable() {
    return rebaseable != null && rebaseable;
  }

  @NotNull
  public String getBodyHTML() {
    return bodyHtml;
  }

  @NotNull
  public Tag getHead() {
    return head;
  }

  @NotNull
  public Tag getBase() {
    return base;
  }

  @NotNull
  public String getHeadLabel() {
    if(head.getRepo() != null){
      return head.getRepo().getFullName() + ":" + head.getRef();
    }

    return ":" + head.getRef();
  }

  @NotNull
  public String getBaseRefName() {
    return base.getRef();
  }
}
