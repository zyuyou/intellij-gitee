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
package com.gitee.authentication.accounts

import com.gitee.api.GiteeServerPath
import com.intellij.internal.statistic.beans.UsageDescriptor
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector
import com.intellij.internal.statistic.utils.getBooleanUsage
import com.intellij.internal.statistic.utils.getCountingUsage
import com.intellij.openapi.util.text.StringUtil

private const val GROUP_ID = "statistics.vcs.gitee"

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/authentication/accounts/GiteeAccountsStatisticsCollector.kt
 * @author JetBrains s.r.o.
 */
class GiteeAccountsStatisticsCollector internal constructor(private val accountManager: GiteeAccountManager)
  : ApplicationUsagesCollector() {

  override fun getUsages(): Set<UsageDescriptor> {
    val hasAccountsWithNonDefaultHost = accountManager.accounts.any {
      !StringUtil.equalsIgnoreCase(it.server.host, GiteeServerPath.DEFAULT_HOST)
    }

    return setOf(getCountingUsage("gitee.accounts.count", accountManager.accounts.size, listOf(0, 1, 2)),
      getBooleanUsage("gitee.accounts.not.default.host", hasAccountsWithNonDefaultHost))
  }

  override fun getGroupId(): String = GROUP_ID
}