// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.pullrequest

import com.gitee.api.data.GENodes
import com.intellij.collaboration.api.dto.GraphQLFragment

@GraphQLFragment("/graphql/fragment/pullRequestMergeability.graphql")
class GEPullRequestMergeabilityData(val mergeable: GEPullRequestMergeableState,
                                    val canBeRebased: Boolean,
                                    val mergeStateStatus: GEPullRequestMergeStateStatus,
                                    val commits: GENodes<GEPullRequestCommitWithCheckStatuses>
)