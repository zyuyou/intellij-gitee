// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.tasks

import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GELoginRequest
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.exceptions.GiteeParseException
import com.intellij.openapi.project.Project

private object GERepositoryEditorKt {
  fun askToken(project: Project, host: String): String? {
    val server = tryParse(host) ?: return null

    return GiteeAuthenticationManager.getInstance().login(
      project, null,
      GELoginRequest(server = server)
    )?.token
  }

  fun askTokens(project: Project, host: String): Pair<String, String>? {
    val server = tryParse(host) ?: return null

    return GiteeAuthenticationManager.getInstance().login(
      project, null,
      GELoginRequest(server = server)
    )?.token?.let {
      val tokens = it.split("&")
      if(tokens.size == 1) {
        it to ""
      } else {
        tokens[0] to tokens[1]
      }
    }
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