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

import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.mandatory.RestModel;

@RestModel
@SuppressWarnings("UnusedDeclaration")
public class GiteeRepoDetailed extends GiteeRepoWithPermissions {
  private GiteeRepo parent;
  private GiteeRepo source;

  private Boolean allowSquashMerge;
  private Boolean allowMergeCommit;
  private Boolean allowRebaseMerge;

  @Nullable
  public GiteeRepo getParent() {
    return parent;
  }

  @Nullable
  public GiteeRepo getSource() {
    return source;
  }

  public boolean getAllowSquashMerge() {
    return allowSquashMerge != null ? allowSquashMerge : false;
  }

  public boolean getAllowMergeCommit() {
    return allowMergeCommit != null ? allowMergeCommit : false;
  }

  public boolean getAllowRebaseMerge() {
    return allowRebaseMerge != null ? allowRebaseMerge : false;
  }
}
