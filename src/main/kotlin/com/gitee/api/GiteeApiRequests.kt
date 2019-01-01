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
import com.gitee.api.requests.*
import com.gitee.api.util.GiteeApiPagesLoader
import com.gitee.api.util.GiteeApiUrlQueryBuilder
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
      fun pages(server: GiteeServerPath, allAssociated: Boolean = true, pagination: GiteeRequestPagination? = GiteeRequestPagination()) =
        GiteeApiPagesLoader.Request(get(server, allAssociated, pagination), ::get)

      @JvmOverloads
      @JvmStatic
      fun get(server: GiteeServerPath, allAssociated: Boolean = true, pagination: GiteeRequestPagination? = null) =
        get(getUrl(server, CurrentUser.urlSuffix, urlSuffix, getQuery(if (allAssociated) "" else "type=owner", pagination?.toString().orEmpty())))

      @JvmStatic
      fun get(url: String) = Get.jsonPage2<GiteeRepo>(url).withOperationName("get user repositories")

      @JvmStatic
      fun create(server: GiteeServerPath, name: String, description: String, private: Boolean, autoInit: Boolean? = null) =
        Post.json<GiteeRepo>(
          getUrl(server, CurrentUser.urlSuffix, urlSuffix),
          GiteeRepoRequest(name, description, private, autoInit)
        ).withOperationName("create user repository")
    }

    object RepoSubs : Entity("/subscriptions") {
      @JvmStatic
      fun pages(server: GiteeServerPath) = GiteeApiPagesLoader.Request(get(server, GiteeRequestPagination()), ::get)

      @JvmOverloads
      @JvmStatic
      fun get(server: GiteeServerPath, pagination: GiteeRequestPagination? = null) =
        get(getUrl(server, CurrentUser.urlSuffix, urlSuffix, getQuery(pagination?.toString().orEmpty())))

      @JvmStatic
      fun get(url: String) = Get.jsonPage2<GiteeRepo>(url).withOperationName("get repository subscriptions")
    }
  }

  object Organisations : Entity("/orgs") {

    object Repos : Entity("/repos") {
      @JvmStatic
      fun pages(server: GiteeServerPath, organisation: String) = GiteeApiPagesLoader.Request(get(server, organisation), ::get)

      @JvmOverloads
      @JvmStatic
      fun get(server: GiteeServerPath, organisation: String, pagination: GiteeRequestPagination? = null) =
        get(getUrl(server, Organisations.urlSuffix, "/", organisation, urlSuffix, pagination?.toString().orEmpty()))

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
    fun delete(url: String) = Delete(url).withOperationName("delete repository at $url")

    object Branches : Entity("/branches") {
      @JvmOverloads
      @JvmStatic
      fun get(server: GiteeServerPath, username: String, repoName: String, pagination: GiteeRequestPagination? = null) =
        get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, getQuery(pagination?.toString().orEmpty())))

      @JvmStatic
      fun get(url: String) = Get.jsonList<GiteeBranch>(url).withOperationName("get branches")
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
      fun get(url: String) = Get.jsonPage2<GiteeRepo>(url).withOperationName("get forks")
    }

    object Issues : Entity("/issues") {
      @JvmStatic
      fun pages(server: GiteeServerPath, username: String, repoName: String, state: String? = null, assignee: String? = null) =
        GiteeApiPagesLoader.Request(get(server, username, repoName, state, assignee, GiteeRequestPagination()), ::get)

      @JvmStatic
      fun get(server: GiteeServerPath, username: String, repoName: String, state: String? = null, assignee: String? = null, pagination: GiteeRequestPagination? = null) =
        get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, GiteeApiUrlQueryBuilder.urlQuery { param("state", state); param("assignee", assignee); param(pagination) }))

      @JvmStatic
      fun get(url: String) = Get.jsonPage2<GiteeIssue>(url).withOperationName("get issues in repository")

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

      object Comments : Entity("/comments") {
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
        fun get(url: String) = Get.jsonPage2<GiteeIssueComment>(url).withOperationName("get comments for issue")
      }
    }

    object PullRequests : Entity("/pulls") {
      @JvmStatic
      fun get(server: GiteeServerPath, username: String, repoName: String, query: String) =
        get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, query))

      @JvmStatic
      fun get(server: GiteeServerPath, username: String, repoName: String) =
        get(server, username, repoName, null, GiteeRequestPagination())

      @JvmStatic
      fun get(server: GiteeServerPath, username: String, repoName: String, state: String? = null, pagination: GiteeRequestPagination? = null) =
        get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, GiteeApiUrlQueryBuilder.urlQuery { param("state", state); param(pagination) }))

      @JvmStatic
      fun get(url: String) = Get.jsonPage2<GiteePullRequest>(url).withOperationName("get pull request")

      @JvmStatic
      fun getHtml(url: String) = Get.json<GiteePullRequestDetailedWithHtml>(url, GiteeApiContentHelper.V3_HTML_JSON_MIME_TYPE)
        .withOperationName("get pull request")

      @JvmStatic
      fun create(server: GiteeServerPath, username: String, repoName: String, title: String, description: String, head: String, base: String) =
        Post.json<GiteePullRequest>(
          getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix),
          GiteePullRequestRequest(username, repoName, title, description, head, base)
        ).withOperationName("create pull request in $username/$repoName")

      @JvmStatic
      fun merge(pullRequest: GiteePullRequest, commitSubject: String, commitBody: String, headSha: String) =
        Put.json<Unit>(getMergeUrl(pullRequest),
          GiteePullRequestMergeRequest(commitSubject, commitBody, headSha, GiteePullRequestMergeMethod.merge))
          .withOperationName("merge pull request ${pullRequest.number}")

      @JvmStatic
      fun squashMerge(pullRequest: GiteePullRequest, commitSubject: String, commitBody: String, headSha: String) =
        Put.json<Unit>(getMergeUrl(pullRequest),
          GiteePullRequestMergeRequest(commitSubject, commitBody, headSha, GiteePullRequestMergeMethod.squash))
          .withOperationName("squash and merge pull request ${pullRequest.number}")

      @JvmStatic
      fun rebaseMerge(pullRequest: GiteePullRequest, headSha: String) =
        Put.json<Unit>(getMergeUrl(pullRequest),
          GiteePullRequestMergeRebaseRequest(headSha))
          .withOperationName("rebase and merge pull request ${pullRequest.number}")

      private fun getMergeUrl(pullRequest: GiteePullRequest) = pullRequest.url + "/merge"
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
    fun delete(server: GiteeServerPath, id: String) = Delete(getUrl(server, urlSuffix, "/$id"))
      .withOperationName("delete gist $id")
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

  private fun getUrl(server: GiteeServerPath, suffix: String) = server.toApiUrl() + suffix

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