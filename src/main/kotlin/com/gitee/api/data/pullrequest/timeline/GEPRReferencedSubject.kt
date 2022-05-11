// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.pullrequest.timeline

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.gitee.api.data.GiteeIssueState
import com.gitee.api.data.pullrequest.GEPullRequestState

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "__typename", visible = false)
@JsonSubTypes(
  JsonSubTypes.Type(name = "Issue", value = GEPRReferencedSubject.Issue::class),
  JsonSubTypes.Type(name = "PullRequest", value = GEPRReferencedSubject.PullRequest::class)
)
sealed class GEPRReferencedSubject(val title: String, val number: Long, val url: String) {

  class Issue(title: String, number: Long, url: String, val state: GiteeIssueState)
    : GEPRReferencedSubject(title, number, url)

  class PullRequest(title: String, number: Long, url: String, val state: GEPullRequestState, val isDraft: Boolean)
    : GEPRReferencedSubject(title, number, url)
}