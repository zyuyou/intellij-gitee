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
package com.gitee.authentication.accounts

import com.gitee.api.GiteeServerPath
import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.beans.newMetric
import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector
import com.intellij.openapi.components.service
import com.intellij.openapi.util.text.StringUtil

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/authentication/accounts/GiteeAccountsStatisticsCollector.kt
 * @author JetBrains s.r.o.
 */
class GiteeAccountsStatisticsCollector : ApplicationUsagesCollector() {

  override fun getVersion() = 2

  override fun getMetrics(): Set<MetricEvent> {
    val accountManager = service<GiteeAccountManager>()
    val hasAccountsWithNonDefaultHost = accountManager.accounts.any {
      !StringUtil.equalsIgnoreCase(it.server.host, GiteeServerPath.DEFAULT_HOST)
    }

    return setOf(
      newMetric("accounts", FeatureUsageData()
        .addCount(accountManager.accounts.size)
        .addData("has_enterprise", hasAccountsWithNonDefaultHost)))
  }

  override fun getGroupId(): String = "vcs.gitee"
}