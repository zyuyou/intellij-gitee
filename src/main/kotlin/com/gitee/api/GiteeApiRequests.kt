/*
 * Copyright 2016-2018 码云 - Gitee
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
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/api/GithubApiRequests.kt
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
      fun pages(server: GiteeServerPath, allAssociated: Boolean = true) =
        GiteeApiPagesLoader.Request(get(server, allAssociated, GiteeRequestPagination()), ::get)

      @JvmOverloads
      @JvmStatic
      fun get(server: GiteeServerPath, allAssociated: Boolean = true, pagination: GiteeRequestPagination? = null) =
        get(getUrl(server, CurrentUser.urlSuffix, urlSuffix, getQuery(if (allAssociated) "" else "type=owner", pagination?.toString().orEmpty())))

      @JvmStatic
      fun get(url: String) = Get.jsonPage2<GiteeRepo>(url).withOperationName("get user repositories")

      @JvmStatic
      fun create(server: GiteeServerPath, name: String, description: String, private: Boolean) =
        Post.json<GiteeRepo>(
          getUrl(server, CurrentUser.urlSuffix, urlSuffix),
          GiteeRepoRequest(name, description, private)
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

  object Repos : Entity("/repos") {
    @JvmStatic
    fun get(server: GiteeServerPath, username: String, repoName: String) =
      Get.Optional.json<GiteeRepoDetailed>(getUrl(server, urlSuffix, "/$username/$repoName"))
        .withOperationName("get information for repository $username/$repoName")

    @JvmStatic
    fun delete(server: GiteeServerPath, username: String, repoName: String) =
      Delete(getUrl(server, urlSuffix, "/$username/$repoName")).withOperationName("delete repository $username/$repoName")

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
        Patch.json<GiteeIssue>(getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix, "/", id), GiteeChangeIssueStateRequest(if (open) "open" else "closed"))

      object Comments : Entity("/comments") {
        @JvmStatic
        fun pages(server: GiteeServerPath, username: String, repoName: String, issueId: String) =
          GiteeApiPagesLoader.Request(get(server, username, repoName, issueId, GiteeRequestPagination()), ::get)

        @JvmStatic
        fun get(server: GiteeServerPath, username: String, repoName: String, issueId: String, pagination: GiteeRequestPagination? = null) =
          get(getUrl(server, Repos.urlSuffix, "/$username/$repoName", Issues.urlSuffix, "/", issueId, urlSuffix, GiteeApiUrlQueryBuilder.urlQuery { param(pagination) }))

        @JvmStatic
        fun get(url: String) = Get.jsonPage2<GiteeIssueComment>(url).withOperationName("get comments for issue")
      }
    }

    object PullRequests : Entity("/pulls") {
      @JvmStatic
      fun create(server: GiteeServerPath, username: String, repoName: String, title: String, description: String, head: String, base: String) =
        Post.json<GiteePullRequest>(
          getUrl(server, Repos.urlSuffix, "/$username/$repoName", urlSuffix),
          GiteePullRequestRequest(username, repoName, title, description, head, base)
        ).withOperationName("create pull request in $username/$repoName")
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

  private fun getUrl(server: GiteeServerPath, vararg suffixes: String) = StringBuilder(server.toApiUrl()).append(*suffixes).toString()

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