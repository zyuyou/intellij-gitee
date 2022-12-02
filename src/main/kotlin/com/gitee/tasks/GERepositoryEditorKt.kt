// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.tasks

import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GEAccountsUtil
import com.gitee.authentication.GECredentials
import com.gitee.authentication.GELoginRequest
import com.gitee.authentication.ui.GELoginModel
import com.gitee.exceptions.GiteeParseException
import com.intellij.openapi.project.Project

private object GERepositoryEditorKt {
  fun askCredentials(project: Project, host: String): GECredentials? {
    val server = tryParse(host) ?: return null

    val model = object : GELoginModel {
      var credentials: GECredentials? = null

      override fun isAccountUnique(server: GiteeServerPath, login: String): Boolean = true

      override suspend fun saveLogin(server: GiteeServerPath, login: String, credentials: GECredentials) {
        this.credentials = credentials
      }
    }
    GEAccountsUtil.login(model, GELoginRequest(server = server), project, null)
    return model.credentials
  }

  private fun tryParse(host: String): GiteeServerPath? {
    return try {
      GiteeServerPath.from(host)
    }
    catch (ignored: GiteeParseException) {
      null
    }
  }
}