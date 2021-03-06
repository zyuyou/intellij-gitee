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
import org.jetbrains.io.mandatory.Mandatory;
import org.jetbrains.io.mandatory.RestModel;

//region Issue label
/*{
    "id": 208045946,
    "node_id": "MDU6TGFiZWwyMDgwNDU5NDY=",
    "url": "https://api.github.com/repos/octocat/Hello-World/labels/bug",
    "name": "bug",
    "description": "Houston, we have a problem",
    "color": "f29513",
    "default": true
  }*/
//endregion
@RestModel
@SuppressWarnings("UnusedDeclaration")
public class GiteeIssueLabel {
  private Long id;
  private String repositoryId;
  private String url;
  @Mandatory
  private String name;
  private String description;
  @Mandatory
  private String color;

  @NotNull
  public String getNodeId() {
    return "nodeId";
  }

  @NotNull
  public String getUrl() {
    return url;
  }

  @NotNull
  public String getName() {
    return name;
  }

  @NotNull
  public String getColor() {
    return color;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof GiteeIssueLabel)) return false;

    GiteeIssueLabel label = (GiteeIssueLabel)o;

    if (!id.equals(label.id)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }
}
