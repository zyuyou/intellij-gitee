// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api

import com.gitee.api.GiteeApiRequest.Post.GQLQuery
import com.gitee.api.data.*
import com.gitee.api.data.graphql.GEGQLRequestPagination
import com.gitee.api.data.graphql.query.GEGQLSearchQueryResponse
import com.gitee.api.data.pullrequest.*
import com.gitee.api.data.pullrequest.timeline.GEPRTimelineItem
import com.gitee.api.data.request.GEPullRequestDraftReviewComment
import com.gitee.api.data.request.GEPullRequestDraftReviewThread
import com.gitee.api.util.GESchemaPreview
import com.intellij.collaboration.api.dto.GraphQLCursorPageInfoDTO
import com.intellij.collaboration.api.dto.GraphQLPagedResponseDataDTO
import com.intellij.diff.util.Side

object GEGQLRequests {
  object Organization {

    object Team {
      fun findAll(server: GiteeServerPath, organization: String,
                  pagination: GEGQLRequestPagination? = null): GQLQuery<GraphQLPagedResponseDataDTO<GETeam>> {

        return GQLQuery.TraversedParsed(server.toGraphQLUrl(), GEGQLQueries.findOrganizationTeams,
          mapOf("organization" to organization,
            "pageSize" to pagination?.pageSize,
            "cursor" to pagination?.afterCursor),
          TeamsConnection::class.java,
          "organization", "teams")
      }

      fun findByUserLogins(server: GiteeServerPath, organization: String, logins: List<String>,
                           pagination: GEGQLRequestPagination? = null): GQLQuery<GraphQLPagedResponseDataDTO<GETeam>> =
        GQLQuery.TraversedParsed(server.toGraphQLUrl(), GEGQLQueries.findOrganizationTeams,
          mapOf("organization" to organization,
            "logins" to logins,
            "pageSize" to pagination?.pageSize,
            "cursor" to pagination?.afterCursor),
          TeamsConnection::class.java,
          "organization", "teams")

      private class TeamsConnection(pageInfo: GraphQLCursorPageInfoDTO, nodes: List<GETeam>)
        : GEConnection<GETeam>(pageInfo, nodes)
    }
  }

  object Repo {
    fun find(repository: GERepositoryCoordinates): GQLQuery<GERepository?> {
      return GQLQuery.OptionalTraversedParsed(repository.serverPath.toGraphQLUrl(), GEGQLQueries.findRepository,
        mapOf(
          "repoOwner" to repository.repositoryPath.owner,
          "repoName" to repository.repositoryPath.repository
        ),
        GERepository::class.java,
        "repository"
      )
    }

    fun getProtectionRules(repository: GERepositoryCoordinates,
                           pagination: GEGQLRequestPagination? = null): GQLQuery<GraphQLPagedResponseDataDTO<GEBranchProtectionRule>> {
      return GQLQuery.TraversedParsed(repository.serverPath.toGraphQLUrl(), GEGQLQueries.getProtectionRules,
        mapOf("repoOwner" to repository.repositoryPath.owner,
          "repoName" to repository.repositoryPath.repository,
          "pageSize" to pagination?.pageSize,
          "cursor" to pagination?.afterCursor),
        ProtectedRulesConnection::class.java,
        "repository", "branchProtectionRules")
    }

    private class ProtectedRulesConnection(pageInfo: GraphQLCursorPageInfoDTO, nodes: List<GEBranchProtectionRule>)
      : GEConnection<GEBranchProtectionRule>(pageInfo, nodes)
  }

  object Comment {

    fun updateComment(server: GiteeServerPath, commentId: String, newText: String): GQLQuery<GEComment> =
      GQLQuery.TraversedParsed(server.toGraphQLUrl(), GEGQLQueries.updateIssueComment,
        mapOf("id" to commentId,
          "body" to newText),
        GEComment::class.java,
        "updateIssueComment", "issueComment")

    fun deleteComment(server: GiteeServerPath, commentId: String): GQLQuery<Any?> =
      GQLQuery.TraversedParsed(server.toGraphQLUrl(), GEGQLQueries.deleteIssueComment,
        mapOf("id" to commentId),
        Any::class.java)
  }

  object PullRequest {
    fun create(repository: GERepositoryCoordinates,
               repositoryId: String,
               baseRefName: String,
               headRefName: String,
               title: String,
               body: String? = null,
               draft: Boolean? = false): GQLQuery<GEPullRequestShort> {
      return GQLQuery.TraversedParsed(repository.serverPath.toGraphQLUrl(), GEGQLQueries.createPullRequest,
        mapOf("repositoryId" to repositoryId,
          "baseRefName" to baseRefName,
          "headRefName" to headRefName,
          "title" to title,
          "body" to body,
          "draft" to draft),
        GEPullRequestShort::class.java,
        "createPullRequest", "pullRequest").apply {
        acceptMimeType = GESchemaPreview.PR_DRAFT.mimeType
      }
    }

    fun findOne(repository: GERepositoryCoordinates, number: Long): GQLQuery<GEPullRequest?> {
      return GQLQuery.OptionalTraversedParsed(repository.serverPath.toGraphQLUrl(), GEGQLQueries.findPullRequest,
        mapOf("repoOwner" to repository.repositoryPath.owner,
          "repoName" to repository.repositoryPath.repository,
          "number" to number),
        GEPullRequest::class.java,
        "repository", "pullRequest").apply {
        acceptMimeType = GESchemaPreview.PR_DRAFT.mimeType
      }
    }

    fun findByBranches(repository: GERepositoryCoordinates, baseBranch: String, headBranch: String)
      : GQLQuery<GraphQLPagedResponseDataDTO<GEPullRequest>> =
      GQLQuery.TraversedParsed(repository.serverPath.toGraphQLUrl(), GEGQLQueries.findOpenPullRequestsByBranches,
        mapOf("repoOwner" to repository.repositoryPath.owner,
          "repoName" to repository.repositoryPath.repository,
          "baseBranch" to baseBranch,
          "headBranch" to headBranch),
        PullRequestsConnection::class.java,
        "repository", "pullRequests").apply {
        acceptMimeType = GESchemaPreview.PR_DRAFT.mimeType
      }

    private class PullRequestsConnection(pageInfo: GraphQLCursorPageInfoDTO, nodes: List<GEPullRequest>)
      : GEConnection<GEPullRequest>(pageInfo, nodes)

    fun update(repository: GERepositoryCoordinates, pullRequestId: String, title: String?, description: String?): GQLQuery<GEPullRequest> {
      val parameters = mutableMapOf<String, Any>("pullRequestId" to pullRequestId)
      if (title != null) parameters["title"] = title
      if (description != null) parameters["body"] = description
      return GQLQuery.TraversedParsed(repository.serverPath.toGraphQLUrl(), GEGQLQueries.updatePullRequest, parameters,
        GEPullRequest::class.java,
        "updatePullRequest", "pullRequest").apply {
        acceptMimeType = GESchemaPreview.PR_DRAFT.mimeType
      }
    }

    fun markReadyForReview(repository: GERepositoryCoordinates, pullRequestId: String): GQLQuery<Any?> =
      GQLQuery.Parsed(repository.serverPath.toGraphQLUrl(), GEGQLQueries.markPullRequestReadyForReview,
        mutableMapOf<String, Any>("pullRequestId" to pullRequestId),
        Any::class.java).apply {
        acceptMimeType = GESchemaPreview.PR_DRAFT.mimeType
      }

    fun mergeabilityData(repository: GERepositoryCoordinates, number: Long): GQLQuery<GEPullRequestMergeabilityData?> =
      GQLQuery.OptionalTraversedParsed(repository.serverPath.toGraphQLUrl(), GEGQLQueries.pullRequestMergeabilityData,
        mapOf("repoOwner" to repository.repositoryPath.owner,
          "repoName" to repository.repositoryPath.repository,
          "number" to number),
        GEPullRequestMergeabilityData::class.java,
        "repository", "pullRequest").apply {
        acceptMimeType = "${GESchemaPreview.CHECKS.mimeType},${GESchemaPreview.PR_MERGE_INFO.mimeType}"
      }

    fun search(server: GiteeServerPath, query: String, pagination: GEGQLRequestPagination? = null)
      : GQLQuery<GEGQLSearchQueryResponse<GEPullRequestShort>> {

      return GQLQuery.Parsed(server.toGraphQLUrl(), GEGQLQueries.issueSearch,
        mapOf("query" to query,
          "pageSize" to pagination?.pageSize,
          "cursor" to pagination?.afterCursor),
        PRSearch::class.java).apply {
        acceptMimeType = GESchemaPreview.PR_DRAFT.mimeType
      }
    }

    private class PRSearch(search: SearchConnection<GEPullRequestShort>)
      : GEGQLSearchQueryResponse<GEPullRequestShort>(search)

    fun reviewThreads(
      repository: GERepositoryCoordinates,
      number: Long,
      pagination: GEGQLRequestPagination? = null
    ): GQLQuery<GraphQLPagedResponseDataDTO<GEPullRequestReviewThread>> =
      GQLQuery.TraversedParsed(
        repository.serverPath.toGraphQLUrl(), GEGQLQueries.pullRequestReviewThreads,
        parameters(repository, number, pagination),
        ThreadsConnection::class.java, "repository", "pullRequest", "reviewThreads"
      )

    private class ThreadsConnection(pageInfo: GraphQLCursorPageInfoDTO, nodes: List<GEPullRequestReviewThread>)
      : GEConnection<GEPullRequestReviewThread>(pageInfo, nodes)

    fun commits(
      repository: GERepositoryCoordinates,
      number: Long,
      pagination: GEGQLRequestPagination? = null
    ): GQLQuery<GraphQLPagedResponseDataDTO<GEPullRequestCommit>> =
      GQLQuery.TraversedParsed(
        repository.serverPath.toGraphQLUrl(), GEGQLQueries.pullRequestCommits,
        parameters(repository, number, pagination),
        CommitsConnection::class.java, "repository", "pullRequest", "commits"
      )

    private class CommitsConnection(pageInfo: GraphQLCursorPageInfoDTO, nodes: List<GEPullRequestCommit>)
      : GEConnection<GEPullRequestCommit>(pageInfo, nodes)

    fun files(
      repository: GERepositoryCoordinates,
      number: Long,
      pagination: GEGQLRequestPagination
    ): GQLQuery<GraphQLPagedResponseDataDTO<GEPullRequestChangedFile>> =
      GQLQuery.TraversedParsed(
        repository.serverPath.toGraphQLUrl(), GEGQLQueries.pullRequestFiles,
        parameters(repository, number, pagination),
        FilesConnection::class.java, "repository", "pullRequest", "files"
      )

    private class FilesConnection(pageInfo: GraphQLCursorPageInfoDTO, nodes: List<GEPullRequestChangedFile>) :
      GEConnection<GEPullRequestChangedFile>(pageInfo, nodes)

    fun markFileAsViewed(server: GiteeServerPath, pullRequestId: String, path: String): GQLQuery<Unit> =
      GQLQuery.TraversedParsed(
        server.toGraphQLUrl(), GEGQLQueries.markFileAsViewed,
        mapOf("pullRequestId" to pullRequestId, "path" to path),
        Unit::class.java
      )

    fun unmarkFileAsViewed(server: GiteeServerPath, pullRequestId: String, path: String): GQLQuery<Unit> =
      GQLQuery.TraversedParsed(
        server.toGraphQLUrl(), GEGQLQueries.unmarkFileAsViewed,
        mapOf("pullRequestId" to pullRequestId, "path" to path),
        Unit::class.java
      )

    object Timeline {
      fun items(server: GiteeServerPath, repoOwner: String, repoName: String, number: Long,
                pagination: GEGQLRequestPagination? = null)
        : GQLQuery<GraphQLPagedResponseDataDTO<GEPRTimelineItem>> {

        return GQLQuery.TraversedParsed(server.toGraphQLUrl(), GEGQLQueries.pullRequestTimeline,
          mapOf("repoOwner" to repoOwner,
            "repoName" to repoName,
            "number" to number,
            "pageSize" to pagination?.pageSize,
            "cursor" to pagination?.afterCursor,
            "since" to pagination?.since),
          TimelineConnection::class.java,
          "repository", "pullRequest", "timelineItems"
        ).apply {
          acceptMimeType = GESchemaPreview.PR_DRAFT.mimeType
        }
      }

      private class TimelineConnection(pageInfo: GraphQLCursorPageInfoDTO, nodes: List<GEPRTimelineItem>)
        : GEConnection<GEPRTimelineItem>(pageInfo, nodes)
    }

    object Review {

      fun create(server: GiteeServerPath, pullRequestId: String,
                 event: GEPullRequestReviewEvent?, body: String?, commitSha: String?,
                 comments: List<GEPullRequestDraftReviewComment>?,
                 threads: List<GEPullRequestDraftReviewThread>?): GQLQuery<GEPullRequestPendingReview> =
        GQLQuery.TraversedParsed(server.toGraphQLUrl(), GEGQLQueries.createReview,
          mapOf("pullRequestId" to pullRequestId,
            "event" to event,
            "commitOid" to commitSha,
            "comments" to comments,
            "threads" to threads,
            "body" to body),
          GEPullRequestPendingReview::class.java,
          "addPullRequestReview", "pullRequestReview")

      fun submit(server: GiteeServerPath, reviewId: String, event: GEPullRequestReviewEvent, body: String?): GQLQuery<Any> =
        GQLQuery.TraversedParsed(server.toGraphQLUrl(), GEGQLQueries.submitReview,
          mapOf("reviewId" to reviewId,
            "event" to event,
            "body" to body),
          Any::class.java)

      fun updateBody(server: GiteeServerPath, reviewId: String, newText: String): GQLQuery<GEPullRequestReview> =
        GQLQuery.TraversedParsed(server.toGraphQLUrl(), GEGQLQueries.updateReview,
          mapOf("reviewId" to reviewId,
            "body" to newText),
          GEPullRequestReview::class.java,
          "updatePullRequestReview", "pullRequestReview")

      fun delete(server: GiteeServerPath, reviewId: String): GQLQuery<Any> =
        GQLQuery.TraversedParsed(server.toGraphQLUrl(), GEGQLQueries.deleteReview,
          mapOf("reviewId" to reviewId),
          Any::class.java)

      fun pendingReviews(server: GiteeServerPath, pullRequestId: String): GQLQuery<GENodes<GEPullRequestPendingReview>> {
        return GQLQuery.TraversedParsed(server.toGraphQLUrl(), GEGQLQueries.pendingReview,
          mapOf("pullRequestId" to pullRequestId),
          PendingReviewNodes::class.java,
          "node", "reviews")
      }

      private class PendingReviewNodes(nodes: List<GEPullRequestPendingReview>) :
        GENodes<GEPullRequestPendingReview>(nodes)

      fun addComment(server: GiteeServerPath,
                     reviewId: String,
                     body: String, commitSha: String, fileName: String, diffLine: Int)
        : GQLQuery<GEPullRequestReviewCommentWithPendingReview> =
        GQLQuery.TraversedParsed(server.toGraphQLUrl(), GEGQLQueries.addReviewComment,
          mapOf("reviewId" to reviewId,
            "body" to body,
            "commit" to commitSha,
            "file" to fileName,
            "position" to diffLine),
          GEPullRequestReviewCommentWithPendingReview::class.java,
          "addPullRequestReviewComment", "comment")

      fun addComment(server: GiteeServerPath,
                     reviewId: String,
                     inReplyTo: String,
                     body: String)
        : GQLQuery<GEPullRequestReviewCommentWithPendingReview> =
        GQLQuery.TraversedParsed(server.toGraphQLUrl(), GEGQLQueries.addReviewComment,
          mapOf("reviewId" to reviewId,
            "inReplyTo" to inReplyTo,
            "body" to body),
          GEPullRequestReviewCommentWithPendingReview::class.java,
          "addPullRequestReviewComment", "comment")

      fun deleteComment(server: GiteeServerPath, commentId: String): GQLQuery<GEPullRequestPendingReview> =
        GQLQuery.TraversedParsed(server.toGraphQLUrl(), GEGQLQueries.deleteReviewComment,
          mapOf("id" to commentId),
          GEPullRequestPendingReview::class.java,
          "deletePullRequestReviewComment", "pullRequestReview")

      fun updateComment(server: GiteeServerPath, commentId: String, newText: String): GQLQuery<GEPullRequestReviewComment> =
        GQLQuery.TraversedParsed(server.toGraphQLUrl(), GEGQLQueries.updateReviewComment,
          mapOf("id" to commentId,
            "body" to newText),
          GEPullRequestReviewComment::class.java,
          "updatePullRequestReviewComment", "pullRequestReviewComment")

      fun addThread(server: GiteeServerPath, reviewId: String,
                    body: String, line: Int, side: Side, startLine: Int, fileName: String): GQLQuery<GEPullRequestReviewThread> =
        GQLQuery.TraversedParsed(server.toGraphQLUrl(), GEGQLQueries.addPullRequestReviewThread,
          mapOf("body" to body,
            "line" to line,
            "path" to fileName,
            "pullRequestReviewId" to reviewId,
            "side" to side.name,
            "startSide" to side.name,
            "startLine" to startLine),
          GEPullRequestReviewThread::class.java,
          "addPullRequestReviewThread", "thread")

      fun resolveThread(server: GiteeServerPath, threadId: String): GQLQuery<GEPullRequestReviewThread> =
        GQLQuery.TraversedParsed(server.toGraphQLUrl(), GEGQLQueries.resolveReviewThread,
          mapOf("threadId" to threadId),
          GEPullRequestReviewThread::class.java,
          "resolveReviewThread", "thread")

      fun unresolveThread(server: GiteeServerPath, threadId: String): GQLQuery<GEPullRequestReviewThread> =
        GQLQuery.TraversedParsed(server.toGraphQLUrl(), GEGQLQueries.unresolveReviewThread,
          mapOf("threadId" to threadId),
          GEPullRequestReviewThread::class.java,
          "unresolveReviewThread", "thread")
    }
  }
}

private fun parameters(
  repository: GERepositoryCoordinates,
  pullRequestNumber: Long,
  pagination: GEGQLRequestPagination?
): Map<String, Any?> =
  mapOf(
    "repoOwner" to repository.repositoryPath.owner,
    "repoName" to repository.repositoryPath.repository,
    "number" to pullRequestNumber,
    "pageSize" to pagination?.pageSize,
    "cursor" to pagination?.afterCursor
  )