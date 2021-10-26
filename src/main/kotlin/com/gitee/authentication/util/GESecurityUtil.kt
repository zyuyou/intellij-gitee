// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.util

import com.gitee.api.*
import com.gitee.api.data.GiteeAuthenticatedUser
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.Url
import com.intellij.util.Urls.newUrl

object GESecurityUtil {
  private const val REPO_SCOPE = "repo"
  private const val GIST_SCOPE = "gist"
  private const val READ_ORG_SCOPE = "read:org"
  private const val WORKFLOW_SCOPE = "workflow"

  val MASTER_SCOPES = listOf(REPO_SCOPE, GIST_SCOPE, READ_ORG_SCOPE, WORKFLOW_SCOPE)

  const val DEFAULT_CLIENT_NAME = "Gitee Integration Plugin"

  @JvmStatic
  internal fun loadCurrentUserWithScopes(executor: GiteeApiRequestExecutor,
                                         progressIndicator: ProgressIndicator,
                                         server: GiteeServerPath): Pair<GiteeAuthenticatedUser, String?> {
    var scopes: String? = null
    val details = executor.execute(progressIndicator,
                                   object : GiteeApiRequest.Get.Json<GiteeAuthenticatedUser>(
                                     GiteeApiRequests.getUrl(server, GiteeApiRequests.CurrentUser.urlSuffix),
                                     GiteeAuthenticatedUser::class.java) {

                                     override fun extractResult(response: GiteeApiResponse): GiteeAuthenticatedUser {
                                       scopes = response.findHeader("X-OAuth-Scopes")
                                       return super.extractResult(response)
                                     }
                                   }.withOperationName("get profile information"))
    return details to scopes
  }

  @JvmStatic
  internal fun isEnoughScopes(grantedScopes: String): Boolean {
    val scopesArray = grantedScopes.split(", ")
    if (scopesArray.isEmpty()) return false
    if (!scopesArray.contains(REPO_SCOPE)) return false
    if (!scopesArray.contains(GIST_SCOPE)) return false
    if (scopesArray.none { it.endsWith(":org") }) return false

    return true
  }

  internal fun buildNewTokenUrl(server: GiteeServerPath): String {
    val productName = ApplicationNamesInfo.getInstance().fullProductName

    return server
      .append("settings/tokens/new")
      .addParameters(mapOf(
        "description" to "$productName Gitee integration plugin",
        "scopes" to MASTER_SCOPES.joinToString(",")
      ))
      .toExternalForm()
  }

  private fun GiteeServerPath.append(path: String): Url =
    newUrl(getSchema(), host + port?.let { ":$it" }.orEmpty(), suffix.orEmpty() + "/" + path)
}