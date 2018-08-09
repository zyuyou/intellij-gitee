// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.accounts

import com.gitee.api.GiteeServerPath
import com.intellij.internal.statistic.beans.UsageDescriptor
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector
import com.intellij.internal.statistic.utils.getBooleanUsage
import com.intellij.internal.statistic.utils.getCountingUsage
import com.intellij.openapi.util.text.StringUtil

private const val GROUP_ID = "statistics.vcs.gitee"

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