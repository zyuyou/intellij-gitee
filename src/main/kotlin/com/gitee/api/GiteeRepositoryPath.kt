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

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/api/GiteeRepositoryPath.kt
 * @author JetBrains s.r.o.
 */
data class GiteeRepositoryPath(private val serverPath: GiteeServerPath,
                               private val repositoryPath: GiteeFullPath) {
  fun toUrl(): String {
    return serverPath.toUrl() + "/" + repositoryPath.fullName
  }

  override fun toString(): String {
    return "$serverPath/${repositoryPath.fullName}"
  }
}
