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
package com.gitee.api

import com.gitee.api.GiteeApiRequest.*
import com.gitee.api.data.*
import com.gitee.api.data.request.*
import com.gitee.api.requests.AuthorizationCreateRequest
import com.gitee.api.requests.AuthorizationUpdateRequest
import com.gitee.api.requests.GiteeChangeIssueStateRequest
import com.gitee.api.util.GiteeApiPagesLoader
import com.gitee.api.util.GiteeApiSearchQueryBuilder
import com.gitee.api.util.GiteeApiUrlQueryBuilder
import com.intellij.openapi.util.io.StreamUtil
import com.intellij.util.ThrowableConvertor
import java.awt.Image

/**
 * Collection of factory methods for API requests used in plugin
 * TODO: improve url building (DSL?)
 *
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/api/GiteeApiRequests.kt
 * @author JetBrains s.r.o.
 */
object GiteeApiRequests {
  object CurrentUser : Entity("/user") {
    @JvmStatic
    fun get(server: GiteeServerPath) = get(getUrl(server, urlSuffix))

    @JvmStatic
    fun get(url: String) = Get.json<GiteeAuthenticatedUser>(url).withOperationName("get profile information")

    @JvmStatic
    fun getAvatar(url: String) = object : Get<Image>(url) {
      override fun extractResult(response: GiteeApiResponse): Image {
        return response.handleBody(ThrowableConvertor {
          GiteeApiContentHelper.loadImage(it)
        })
      }
    }.withOperationName("get profile avatar")

    object Repos : Entity("/repos") {
      @JvmOverloads
      @JvmStatic
      fun pages(server: GiteeServerPath,
                type: Type = Type.DEFAULT,
                visibility: Visibility = Visibility.DEFAULT,
                affiliation: Affiliation = Affiliation.DEFAULT,
                pagination: GiteeRequestPagination? = null) =
          GiteeApiPagesLoader.Request(get(server, type, visibility, affiliation, pagination), ::get)

      @JvmOverloads
      @JvmStatic
      fun get(server: GiteeServerPath,
              type: Type = Type.DEFAULT,
              visibility: Visibility = Visibility.DEFAULT,
              affiliation: Affiliation = Affiliation.DEFAULT,
              pagination: GiteeRequestPagination? = null): GiteeApiRequest<GiteeResponsePage<GiteeRepo>> {
        if (type != Type.DEFAULT && (visibility != Visibility.DEFAULT || affiliation != Affiliation.DEFAULT)) {
          throw IllegalArgumentException("Param 'type' should not be used together with 'visibility' or 'affiliation'")
        }

        return get(getUrl(server, CurrentUser.urlSuffix, urlSuffix,
            getQuery(type.toString(), visibility.toString(), affiliation.toString(), pagination?.toString().orEmpty())))
      }

      @JvmStatic
      fun get(url: String) = Get.jsonPage<GiteeRepo>(url).withOperationName("get user repositories")

      @JvmStatic
      fun create(server: GiteeServerPath, name: String, description: String, private: Boolean, autoInit: Boolean? = null) =
          Post.json<GiteeRepo>(getUrl(server, CurrentUser.urlSuffix, urlSuffix),
              GiteeRepoRequest(name, description, private, autoInit))
              .withOperationName("create user repository")
    }


    object Orgs : Entity("/orgs") {
      @JvmOverloads
      @JvmStatic
      fun pages(server: GiteeServerPath, pagination: GiteeRequestPagination? = null) =
          GiteeApiPagesLoader.Request(get(server, pagination), ::get)

      fun get(server: GiteeServerPath, pagination: GiteeRequestPagination? = null) =
          get(getUrl(server, CurrentUser.urlSuffix, urlSuffix, getQuery(pagination?.toString().orEmpty())))

      fun get(url: String) = Get.jsonPage<GiteeOrg>(url).withOperationName("get user organizations")
    }

    object RepoSubs : Entity("/subscriptions") {
      @JvmStatic
      fun pages(server: GiteeServerPath) = GiteeApiPagesLoader.Request(get(server, GiteeRequestPagination()), ::get)

      @JvmOverloads
      @JvmStatic
      fun get(server: GiteeServerPath, pagination: GiteeRequestPagination? = null) =
        get(getUrl(server, CurrentUser.urlSuffix, urlSuffix, getQuery(pagination?.toString().orEmpty())))

      @JvmStatic
      fun get(url: String) = Get.jsonPage<GiteeRepo>(url).withOperationName("get repository subscriptions")
    }
  }

  object Organisations : Entity("/orgs") {

    object Repos : Entity("/repos") {
      @JvmStatic
      fun pages(server: GiteeServerPath, organisation: String, pagination: GiteeRequestPagination? = null) =
          GiteeApiPagesLoader.Request(get(server, organisation, pagination), ::get)

      @JvmOverloads
      @JvmStatic
      fun get(server: GiteeServerPath, organisation: String, pagination: GiteeRequestPagination? = null) =
          get(getUrl(server, Organisations.urlSuffix, "/", organisation, urlSuffix, getQuery(pagination?.toString().orEmpty())))

      @JvmStatic
      fun get(url: String) = Get.jsonPage<GiteeRepo>(url).withOperationName("get organisation repositories")

      @JvmStatic
      fun create(server: GiteeServerPath, organisation: String, name: String, description: String, private: Boolean) =
          Post.json<GiteeRepo>(getUrl(server, Organisations.urlSuffix, "/", organisation, urlSuffix),
              GiteeRepoRequest(name, description, private, null))
              .withOperationName("create organisation repository")
    }
  }

  object Repos : Entity("/repos") {
    @JvmStatic
    fun get(server: GiteeServerPath, username: String, repoName: String) =
      Get.Optional.json<GiteeRepoDetailed>(getUrl(server, urlSuffix, "/$username/$repoName"))
        .withOperationName("get information for repository $username/$repoName")

    @JvmStatic
    fun delete(server: GiteeServerPath, username: String, repoName: String) =
      delete(getUrl(server, urlSuffix, "/$username/$repoName")).withOperationName("delete repository $username/$repoName")

    @JvmStatic
    fun delete(url: String) = Delete.json<Unit>(url).withOperationName("delete repository at $url")

    object Branches : Entity("/branches") {
      @JvmStatic
      fun pages(server: GiteeServerPath, username: String, repoName: String) =
          GiteeApiPagesLoader.Request(get(server, username, repoName), ::get)

      @JvmOverloads
      @JvmStatic
      fun get(server: GiteeServerPath, username: String, repoName: String, pagination: GiteeRequestPagination? = null) =
          get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, getQuery(pagination?.toString().orEmpty())))

      @JvmStatic
      fun get(url: String) = Get.jsonPage<GiteeBranch>(url).withOperationName("get branches")
    }

    object Forks : Entity("/forks") {
      @JvmStatic
      fun create(server: GiteeServerPath, username: String, repoName: String) =
        Post.json<GiteeRepo>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix), Any())
          .withOperationName("fork repository $username/$repoName for cuurent user")

      @JvmStatic
      fun pages(server: GiteeServerPath, username: String, repoName: String) =
        GiteeApiPagesLoader.Request(get(server, username, repoName, GiteeRequestPagination()), ::get)

      @JvmOverloads
      @JvmStatic
      fun get(server: GiteeServerPath, username: String, repoName: String, pagination: GiteeRequestPagination? = null) =
        get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, getQuery(pagination?.toString().orEmpty())))

      @JvmStatic
      fun get(url: String) = Get.jsonPage<GiteeRepo>(url).withOperationName("get forks")
    }

    object Assignees : Entity("/assignees") {

      @JvmStatic
      fun pages(server: GiteeServerPath, username: String, repoName: String) =
          GiteeApiPagesLoader.Request(get(server, username, repoName), ::get)

      @JvmOverloads
      @JvmStatic
      fun get(server: GiteeServerPath, username: String, repoName: String, pagination: GiteeRequestPagination? = null) =
          get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, getQuery(pagination?.toString().orEmpty())))

      @JvmStatic
      fun get(url: String) = Get.jsonPage<GiteeUser>(url).withOperationName("get assignees")
    }

    object Labels : Entity("/labels") {

      @JvmStatic
      fun pages(server: GiteeServerPath, username: String, repoName: String) =
          GiteeApiPagesLoader.Request(get(server, username, repoName), ::get)

      @JvmOverloads
      @JvmStatic
      fun get(server: GiteeServerPath, username: String, repoName: String, pagination: GiteeRequestPagination? = null) =
          get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, getQuery(pagination?.toString().orEmpty())))

      @JvmStatic
      fun get(url: String) = Get.jsonPage<GiteeIssueLabel>(url).withOperationName("get assignees")
    }

    object Collaborators : Entity("/collaborators") {

      @JvmStatic
      fun pages(server: GiteeServerPath, username: String, repoName: String) =
          GiteeApiPagesLoader.Request(get(server, username, repoName), ::get)

      @JvmOverloads
      @JvmStatic
      fun get(server: GiteeServerPath, username: String, repoName: String, pagination: GiteeRequestPagination? = null) =
          get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, getQuery(pagination?.toString().orEmpty())))

      @JvmStatic
      fun get(url: String) = Get.jsonPage<GiteeUserWithPermissions>(url).withOperationName("get collaborators")

      @JvmStatic
      fun add(server: GiteeServerPath, username: String, repoName: String, collaborator: String) =
          Put.json<Unit>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, "/", collaborator))
    }

    object Issues : Entity("/issues") {
      @JvmStatic
      fun pages(server: GiteeServerPath, username: String, repoName: String, state: String? = null, assignee: String? = null) =
        GiteeApiPagesLoader.Request(get(server, username, repoName, state, assignee, GiteeRequestPagination()), ::get)

      @JvmStatic
      fun get(server: GiteeServerPath, username: String, repoName: String, state: String? = null, assignee: String? = null, pagination: GiteeRequestPagination? = null) =
        get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, GiteeApiUrlQueryBuilder.urlQuery { param("state", state); param("assignee", assignee); param(pagination) }))

      @JvmStatic
      fun get(url: String) = Get.jsonPage<GiteeIssue>(url).withOperationName("get issues in repository")

      @JvmStatic
      fun get(server: GiteeServerPath, username: String, repoName: String, id: String) =
        Get.Optional.json<GiteeIssue>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, "/", id))

      @JvmStatic
      fun updateState(server: GiteeServerPath, username: String, repoName: String, id: String, open: Boolean) =
        Patch.json<GiteeIssue>(getUrl(server, Repos.urlSuffix, "/$username", urlSuffix, "/", id), GiteeChangeIssueStateRequest(repoName, if (open) "open" else "closed"))

      @JvmStatic
      fun get(server: GiteeServerPath, query: String, pagination: GiteeRequestPagination? = null) =
        get(getUrl(server, Repos.urlSuffix, urlSuffix, GiteeApiUrlQueryBuilder.urlQuery {
          param("q", query)
          param(pagination)
        }))

      @JvmStatic
      fun updateAssignees(server: GiteeServerPath, username: String, repoName: String, id: String, assignees: Collection<String>) =
          Patch.json<GiteeIssue>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, "/", id),
              GiteeAssigneesCollectionRequest(assignees))

      object Comments : Entity("/comments") {
        @JvmStatic
        fun create(repository: GiteeRepositoryCoordinates, issueId: Long, body: String) =
          create(repository.serverPath, repository.repositoryPath.owner, repository.repositoryPath.repository, issueId.toString(), body)

        @JvmStatic
        fun create(server: GiteeServerPath, username: String, repoName: String, issueId: String, body: String) =
          Post.json<GiteeIssueCommentWithHtml>(
            getUrl(server, Repos.urlSuffix, "/$username/$repoName", Issues.urlSuffix, "/", issueId, urlSuffix),
            GiteeCreateIssueCommentRequest(body),
            GiteeApiContentHelper.V3_HTML_JSON_MIME_TYPE)

        @JvmStatic
        fun pages(server: GiteeServerPath, username: String, repoName: String, issueId: String) =
          GiteeApiPagesLoader.Request(get(server, username, repoName, issueId, GiteeRequestPagination()), ::get)

        @JvmStatic
        fun pages(url: String) = GiteeApiPagesLoader.Request(
          get(url + GiteeApiUrlQueryBuilder.urlQuery { param(GiteeRequestPagination()) }), ::get)

        @JvmStatic
        fun get(server: GiteeServerPath, username: String, repoName: String, issueId: String, pagination: GiteeRequestPagination? = null) =
          get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", Issues.urlSuffix, "/", issueId, urlSuffix, GiteeApiUrlQueryBuilder.urlQuery { param(pagination) }))

        @JvmStatic
        fun get(url: String) = Get.jsonPage<GiteeIssueComment>(url).withOperationName("get comments for issue")
      }

      object Labels : Entity("/labels") {
        @JvmStatic
        fun replace(server: GiteeServerPath, username: String, repoName: String, issueId: String, labels: Collection<String>) =
            Put.jsonList<GiteeIssueLabel>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", Issues.urlSuffix, "/", issueId, urlSuffix),
                GiteeLabelsCollectionRequest(labels))
      }
    }

    object PullRequests : Entity("/pulls") {

      @JvmStatic
      fun getDiff(serverPath: GiteeServerPath, username: String, repoName: String, number: Long) =
        object : Get<String>(getUrl(serverPath, Repos.urlSuffix, "/$username/$repoName", urlSuffix, "/$number"),
          GiteeApiContentHelper.V3_DIFF_JSON_MIME_TYPE) {
          override fun extractResult(response: GiteeApiResponse): String {
            return response.handleBody(ThrowableConvertor {
              StreamUtil.readText(it, Charsets.UTF_8)
            })
          }
        }.withOperationName("get pull request diff file")

      @JvmStatic
      fun create(server: GiteeServerPath,
                 username: String, repoName: String,
                 title: String, description: String, head: String, base: String) =
          Post.json<GiteePullRequestDetailed>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix),
              GiteePullRequestRequest(title, description, head, base))
              .withOperationName("create pull request in $username/$repoName")

      @JvmStatic
      fun update(serverPath: GiteeServerPath, username: String, repoName: String, number: Long,
                 title: String? = null,
                 body: String? = null,
                 state: GiteeIssueState? = null,
                 base: String? = null,
                 maintainerCanModify: Boolean? = null) =
          Patch.json<GiteePullRequestDetailed>(getUrl(serverPath, Repos.urlSuffix, "/$username/$repoName", urlSuffix, "/$number"),
              GiteePullUpdateRequest(title, body, state, base, maintainerCanModify))
              .withOperationName("update pull request $number")

      @JvmStatic
      fun update(url: String,
                 title: String? = null,
                 body: String? = null,
                 state: GiteeIssueState? = null,
                 base: String? = null,
                 maintainerCanModify: Boolean? = null) =
          Patch.json<GiteePullRequestDetailed>(url, GiteePullUpdateRequest(title, body, state, base, maintainerCanModify))
              .withOperationName("update pull request")

      @JvmStatic
      fun merge(server: GiteeServerPath, repoPath: GiteeRepositoryPath, number: Long,
                commitSubject: String, commitBody: String, headSha: String) =
          Put.json<Unit>(getUrl(server, Repos.urlSuffix, "/$repoPath", urlSuffix, "/merge"),
              GiteePullRequestMergeRequest(commitSubject, commitBody, headSha, GiteePullRequestMergeMethod.merge))
              .withOperationName("merge pull request ${number}")

      @JvmStatic
      fun squashMerge(server: GiteeServerPath, repoPath: GiteeRepositoryPath, number: Long,
                      commitSubject: String, commitBody: String, headSha: String) =
          Put.json<Unit>(getUrl(server, Repos.urlSuffix, "/$repoPath", urlSuffix, "/merge"),
              GiteePullRequestMergeRequest(commitSubject, commitBody, headSha, GiteePullRequestMergeMethod.squash))
              .withOperationName("squash and merge pull request ${number}")

      @JvmStatic
      fun rebaseMerge(server: GiteeServerPath, repoPath: GiteeRepositoryPath, number: Long,
                      headSha: String) =
          Put.json<Unit>(getUrl(server, Repos.urlSuffix, "/$repoPath", urlSuffix, "/merge"),
              GiteePullRequestMergeRebaseRequest(headSha))
              .withOperationName("rebase and merge pull request ${number}")

      @JvmStatic
      fun getListETag(server: GiteeServerPath, repoPath: GiteeRepositoryPath) =
          object : Get<String?>(getUrl(server, Repos.urlSuffix, "/$repoPath", urlSuffix,
              GiteeApiUrlQueryBuilder.urlQuery { param(GiteeRequestPagination(pageSize = 1)) })) {
            override fun extractResult(response: GiteeApiResponse) = response.findHeader("ETag")
          }.withOperationName("get pull request list ETag")

      object Reviewers : Entity("/requested_reviewers") {
        @JvmStatic
        fun add(server: GiteeServerPath, username: String, repoName: String, number: Long, reviewers: Collection<String>) =
            Post.json<Unit>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", PullRequests.urlSuffix, "/$number", urlSuffix),
                GiteeReviewersCollectionRequest(reviewers, listOf<String>()))

        @JvmStatic
        fun remove(server: GiteeServerPath, username: String, repoName: String, number: Long, reviewers: Collection<String>) =
            Delete.json<Unit>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", PullRequests.urlSuffix, "/$number", urlSuffix),
                GiteeReviewersCollectionRequest(reviewers, listOf<String>()))
      }

      object Commits : Entity("/commits") {
        @JvmStatic
        fun pages(server: GiteeServerPath, username: String, repoName: String, number: Long) =
          GiteeApiPagesLoader.Request(get(server, username, repoName, number), ::get)

        @JvmStatic
        fun pages(url: String) = GiteeApiPagesLoader.Request(get(url), ::get)

        @JvmStatic
        fun get(server: GiteeServerPath, username: String, repoName: String, number: Long,
                pagination: GiteeRequestPagination? = null) =
          get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", PullRequests.urlSuffix, "/$number", urlSuffix,
            GiteeApiUrlQueryBuilder.urlQuery { param(pagination) }))

        @JvmStatic
        fun get(url: String) = Get.jsonPage<GiteeCommit>(url)
            .withOperationName("get commits for pull request")
      }

      object Comments : Entity("/comments") {
        @JvmStatic
        fun pages(server: GiteeServerPath, username: String, repoName: String, number: Long) =
            GiteeApiPagesLoader.Request(get(server, username, repoName, number), ::get)

        @JvmStatic
        fun pages(url: String) = GiteeApiPagesLoader.Request(get(url), ::get)

        @JvmStatic
        fun get(server: GiteeServerPath, username: String, repoName: String, number: Long,
                pagination: GiteeRequestPagination? = null) =
            get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", PullRequests.urlSuffix, "/$number", urlSuffix,
                GiteeApiUrlQueryBuilder.urlQuery { param(pagination) }))

        @JvmStatic
        fun get(url: String) = Get.jsonPage<GiteePullRequestCommentWithHtml>(url, GiteeApiContentHelper.V3_HTML_JSON_MIME_TYPE)
            .withOperationName("get comments for pull request")

        @JvmStatic
        fun createReply(repository: GiteeRepositoryCoordinates, pullRequest: Long, commentId: Long, body: String) =
            Post.json<GiteePullRequestCommentWithHtml>(
                getUrl(repository, PullRequests.urlSuffix, "/$pullRequest", "/comments/$commentId/replies"),
                mapOf("body" to body),
                GiteeApiContentHelper.V3_HTML_JSON_MIME_TYPE).withOperationName("reply to pull request review comment")

        @JvmStatic
        fun create(repository: GiteeRepositoryCoordinates, pullRequest: Long,
                   commitSha: String, filePath: String, diffLine: Int,
                   body: String) =
            Post.json<GiteePullRequestCommentWithHtml>(
                getUrl(repository, PullRequests.urlSuffix, "/$pullRequest", "/comments"),
                mapOf("body" to body,
                    "commit_id" to commitSha,
                    "path" to filePath,
                    "position" to diffLine),
                GiteeApiContentHelper.V3_HTML_JSON_MIME_TYPE).withOperationName("create pull request review comment")
      }
    }
  }

  object Gists : Entity("/gists") {
    @JvmStatic
    fun create(server: GiteeServerPath, contents: List<GiteeGistRequest.FileContent>, description: String, public: Boolean) =
      Post.json<GiteeGist>(
        getUrl(server, urlSuffix),
        GiteeGistRequest(contents, description, public)
      ).withOperationName("create gist")

    @JvmStatic
    fun get(server: GiteeServerPath, id: String) = Get.Optional.json<GiteeGist>(getUrl(server, urlSuffix, "/$id"))
      .withOperationName("get gist $id")

    @JvmStatic
    fun delete(server: GiteeServerPath, id: String) = Delete.json<Unit>(getUrl(server, urlSuffix, "/$id"))
      .withOperationName("delete gist $id")
  }

  object Search : Entity("/search") {
    object Issues : Entity("/issues") {
      @JvmStatic
      fun pages(server: GiteeServerPath, repoPath: GiteeRepositoryPath?, state: String?, assignee: String?, query: String?) =
        GiteeApiPagesLoader.Request(get(server, repoPath, state, assignee, query, GiteeRequestPagination()), ::get)

      @JvmStatic
      fun get(server: GiteeServerPath, repoPath: GiteeRepositoryPath?, state: String?, assignee: String?, query: String?,
              pagination: GiteeRequestPagination? = null) =
        get(getUrl(server, Search.urlSuffix, urlSuffix,
          GiteeApiUrlQueryBuilder.urlQuery {
            param("q", GiteeApiSearchQueryBuilder.searchQuery {
              qualifier("repo", repoPath?.toString().orEmpty())
              qualifier("state", state)
              qualifier("assignee", assignee)
              query(query)
            })
            param(pagination)
          }))

      @JvmStatic
      fun get(server: GiteeServerPath, query: String, pagination: GiteeRequestPagination? = null) =
        get(getUrl(server, Search.urlSuffix, urlSuffix,
          GiteeApiUrlQueryBuilder.urlQuery {
            param("q", query)
            param(pagination)
          }))


      @JvmStatic
      fun get(url: String) = Get.jsonSearchPage<GiteeSearchedIssue>(url).withOperationName("search issues in repository")
    }
  }

  object Auth : Entity("/oauth/token") {
    @JvmStatic
    fun create(server: GiteeServerPath, scopes: List<String>, login: String, password: CharArray) =
      Post.formUrlEncoded<GiteeAuthorization>(
        getBaseUrl(server, urlSuffix),
        AuthorizationCreateRequest(scopes, login, String(password))
      ).withOperationName("create authorization")

    @JvmStatic
    fun update(server: GiteeServerPath, refreshToken: String) =
      Post.formUrlEncoded<GiteeAuthorization>(
        getBaseUrl(server, urlSuffix),
        AuthorizationUpdateRequest(refreshToken)
      ).withOperationName("create authorization")

    @JvmStatic
    fun get(server: GiteeServerPath) = Get.jsonList<GiteeAuthorization>(getUrl(server, urlSuffix))
      .withOperationName("get authorizations")
  }

  abstract class Entity(val urlSuffix: String)

  private fun getBaseUrl(server: GiteeServerPath, suffix: String) = server.toUrl() + suffix

  private fun getUrl(repository: GiteeRepositoryCoordinates, vararg suffixes: String) =
      getUrl(repository.serverPath, Repos.urlSuffix, "/", repository.repositoryPath.toString(), *suffixes)

  fun getUrl(server: GiteeServerPath, vararg suffixes: String) = StringBuilder(server.toApiUrl()).append(*suffixes).toString()

  private fun getQuery(vararg queryParts: String): String {
    val builder = StringBuilder()
    for (part in queryParts) {
      if (part.isEmpty()) continue
      if (builder.isEmpty()) builder.append("?")
      else builder.append("&")
      builder.append(part)
    }
    return builder.toString()
  }
}