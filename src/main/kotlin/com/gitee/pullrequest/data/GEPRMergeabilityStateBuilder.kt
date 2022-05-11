// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data

import com.gitee.api.data.*
import com.gitee.api.data.pullrequest.GEPullRequestMergeStateStatus
import com.gitee.api.data.pullrequest.GEPullRequestMergeabilityData
import com.gitee.api.data.pullrequest.GEPullRequestMergeableState
import com.gitee.pullrequest.data.GEPRMergeabilityState.ChecksState
import com.gitee.pullrequest.data.service.GEPRSecurityService
import com.intellij.util.containers.nullize

class GEPRMergeabilityStateBuilder(private val headRefOid: String, private val prHtmlUrl: String,
                                   private val mergeabilityData: GEPullRequestMergeabilityData
) {

  private var canOverrideAsAdmin = false
  private var requiredContexts = emptyList<String>()
  private var isRestricted = false
  private var requiredApprovingReviewsCount = 0

  fun withRestrictions(securityService: GEPRSecurityService, baseBranchProtectionRules: GEBranchProtectionRules) {
    canOverrideAsAdmin = baseBranchProtectionRules.enforceAdmins?.enabled == false &&
                         securityService.currentUserHasPermissionLevel(GERepositoryPermissionLevel.ADMIN)
    requiredContexts = baseBranchProtectionRules.requiredStatusChecks?.contexts.orEmpty()

    val restrictions = baseBranchProtectionRules.restrictions
    val allowedLogins = restrictions?.users?.map { it.login }.nullize()
    val allowedTeams = restrictions?.teams?.map { it.slug }.nullize()
    isRestricted = (allowedLogins != null && !allowedLogins.contains(securityService.currentUser.login)) ||
                   (allowedTeams != null && !securityService.isUserInAnyTeam(allowedTeams))

    requiredApprovingReviewsCount = baseBranchProtectionRules.requiredPullRequestReviews?.requiredApprovingReviewCount ?: 0
  }

  fun build(): GEPRMergeabilityState {
    val hasConflicts = when (mergeabilityData.mergeable) {
      GEPullRequestMergeableState.MERGEABLE -> false
      GEPullRequestMergeableState.CONFLICTING -> true
      GEPullRequestMergeableState.UNKNOWN -> null
    }

    var failedChecks = 0
    var pendingChecks = 0
    var successfulChecks = 0

    val lastCommit = mergeabilityData.commits.nodes.lastOrNull()?.commit
    val contexts = lastCommit?.status?.contexts.orEmpty()
    for (context in contexts) {
      when (context.state) {
        GECommitStatusContextState.ERROR,
        GECommitStatusContextState.FAILURE -> failedChecks++
        GECommitStatusContextState.EXPECTED,
        GECommitStatusContextState.PENDING -> pendingChecks++
        GECommitStatusContextState.SUCCESS -> successfulChecks++
      }
    }

    val checkSuites = lastCommit?.checkSuites?.nodes.orEmpty()
    for (suite in checkSuites) {
      when (suite.status) {
        GECommitCheckSuiteStatusState.IN_PROGRESS,
        GECommitCheckSuiteStatusState.QUEUED,
        GECommitCheckSuiteStatusState.REQUESTED -> pendingChecks++
        GECommitCheckSuiteStatusState.COMPLETED -> {
          when (suite.conclusion) {
            GECommitCheckSuiteConclusion.ACTION_REQUIRED -> failedChecks++
            GECommitCheckSuiteConclusion.CANCELLED -> successfulChecks++
            GECommitCheckSuiteConclusion.FAILURE -> failedChecks++
            GECommitCheckSuiteConclusion.NEUTRAL -> successfulChecks++
            GECommitCheckSuiteConclusion.SKIPPED -> successfulChecks++
            GECommitCheckSuiteConclusion.STALE -> failedChecks++
            GECommitCheckSuiteConclusion.STARTUP_FAILURE -> failedChecks++
            GECommitCheckSuiteConclusion.SUCCESS -> successfulChecks++
            GECommitCheckSuiteConclusion.TIMED_OUT -> failedChecks++
            null -> failedChecks++
          }
        }
      }
    }

    val canBeMerged = when {
      mergeabilityData.mergeStateStatus.canMerge() -> true
      mergeabilityData.mergeStateStatus.adminCanMerge() && canOverrideAsAdmin -> true
      else -> false
    }

    val summaryChecksState = getChecksSummaryState(failedChecks, pendingChecks, successfulChecks)
    val checksState = when (mergeabilityData.mergeStateStatus) {
      GEPullRequestMergeStateStatus.CLEAN,
      GEPullRequestMergeStateStatus.DIRTY,
      GEPullRequestMergeStateStatus.DRAFT,
      GEPullRequestMergeStateStatus.HAS_HOOKS,
      GEPullRequestMergeStateStatus.UNKNOWN,
      GEPullRequestMergeStateStatus.UNSTABLE -> summaryChecksState
      GEPullRequestMergeStateStatus.BEHIND -> ChecksState.BLOCKING_BEHIND
      GEPullRequestMergeStateStatus.BLOCKED -> {
        if (requiredContexts.isEmpty()
            || contexts
              .filter { it.state == GECommitStatusContextState.SUCCESS }
              .map { it.context }
              .containsAll(requiredContexts)) {
          summaryChecksState
        }
        else ChecksState.BLOCKING_FAILING
      }
    }

    val actualRequiredApprovingReviewsCount =
      if (mergeabilityData.mergeStateStatus == GEPullRequestMergeStateStatus.BLOCKED && !isRestricted && checksState != ChecksState.BLOCKING_FAILING)
        requiredApprovingReviewsCount
      else 0

    return GEPRMergeabilityState(headRefOid, prHtmlUrl,
                                 hasConflicts,
                                 failedChecks, pendingChecks, successfulChecks,
                                 checksState,
                                 canBeMerged, mergeabilityData.canBeRebased,
                                 isRestricted, actualRequiredApprovingReviewsCount)
  }

  private fun getChecksSummaryState(failed: Int, pending: Int, successful: Int): ChecksState {
    return when {
      failed > 0 -> ChecksState.FAILING
      pending > 0 -> ChecksState.PENDING
      successful > 0 -> ChecksState.SUCCESSFUL
      else -> ChecksState.NONE
    }
  }
}