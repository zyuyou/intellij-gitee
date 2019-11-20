// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api

import com.gitee.api.GiteeApiRequest.Post.GQLQuery
import com.gitee.api.data.GEConnection
import com.gitee.api.data.graphql.GiteeGQLPageInfo
import com.gitee.api.data.graphql.GiteeGQLPagedRequestResponse
import com.gitee.api.data.graphql.GiteeGQLRequestPagination
import com.gitee.api.data.graphql.query.GiteeGQLSearchQueryResponse
import com.gitee.api.data.pullrequest.GEPullRequest
import com.gitee.api.data.pullrequest.GEPullRequestReviewThread
import com.gitee.api.data.pullrequest.GEPullRequestShort
import com.gitee.api.data.pullrequest.timeline.GiteePRTimelineItem

object GiteeGQLRequests {
  object PullRequest {
    fun findOne(server: GiteeServerPath, repoOwner: String, repoName: String, number: Long): GQLQuery<GEPullRequest?> {
      return GQLQuery.OptionalTraversedParsed(server.toGraphQLUrl(), GiteeGQLQueries.findPullRequest,
        mapOf("repoOwner" to repoOwner,
          "repoName" to repoName,
          "number" to number),
        GEPullRequest::class.java,
        "repository", "pullRequest")
    }

    fun search(server: GiteeServerPath, query: String, pagination: GiteeGQLRequestPagination? = null)
      : GQLQuery<GiteeGQLSearchQueryResponse<GEPullRequestShort>> {

      return GQLQuery.Parsed(server.toGraphQLUrl(), GiteeGQLQueries.issueSearch,
        mapOf("query" to query,
          "pageSize" to pagination?.pageSize,
          "cursor" to pagination?.afterCursor),
        PRSearch::class.java)
    }

    private class PRSearch(search: SearchConnection<GEPullRequestShort>)
      : GiteeGQLSearchQueryResponse<GEPullRequestShort>(search)

    fun reviewThreads(server: GiteeServerPath,
                      repoOwner: String,
                      repoName: String,
                      number: Long,
                      pagination: GiteeGQLRequestPagination? = null): GQLQuery<GiteeGQLPagedRequestResponse<GEPullRequestReviewThread>> {
      return GQLQuery.TraversedParsed(server.toGraphQLUrl(), GiteeGQLQueries.pullRequestReviewThreads,
        mapOf("repoOwner" to repoOwner,
          "repoName" to repoName,
          "number" to number,
          "pageSize" to pagination?.pageSize,
          "cursor" to pagination?.afterCursor),
        ThreadsConnection::class.java,
        "repository", "pullRequest", "reviewThreads")
    }

    private class ThreadsConnection(pageInfo: GiteeGQLPageInfo, nodes: List<GEPullRequestReviewThread>)
      : GEConnection<GEPullRequestReviewThread>(pageInfo, nodes)

    object Timeline {
      fun items(server: GiteeServerPath, repoOwner: String, repoName: String, number: Long
                , pagination: GiteeGQLRequestPagination? = null)
        : GQLQuery<GiteeGQLPagedRequestResponse<GiteePRTimelineItem>> {

        return GQLQuery.TraversedParsed(server.toGraphQLUrl(), GiteeGQLQueries.pullRequestTimeline,
          mapOf("repoOwner" to repoOwner,
            "repoName" to repoName,
            "number" to number,
            "pageSize" to pagination?.pageSize,
            "cursor" to pagination?.afterCursor),
          TimelineConnection::class.java,
          "repository", "pullRequest", "timelineItems")
      }

      private class TimelineConnection(pageInfo: GiteeGQLPageInfo, nodes: List<GiteePRTimelineItem>)
        : GEConnection<GiteePRTimelineItem>(pageInfo, nodes)
    }
  }
}