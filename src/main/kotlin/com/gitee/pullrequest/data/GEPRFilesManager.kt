// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.data

import com.gitee.api.data.GiteePullRequest
import com.gitee.pullrequest.GENewPRDiffVirtualFile
import com.gitee.pullrequest.GEPRDiffVirtualFile
import com.gitee.pullrequest.GEPRTimelineVirtualFile
import com.intellij.openapi.Disposable

internal interface GEPRFilesManager : Disposable {
  val newPRDiffFile: GENewPRDiffVirtualFile

  fun createAndOpenTimelineFile(pullRequest: GEPRIdentifier, requestFocus: Boolean)

  fun createAndOpenDiffFile(pullRequest: GEPRIdentifier, requestFocus: Boolean)

  fun openNewPRDiffFile(requestFocus: Boolean)

  fun findTimelineFile(pullRequest: GEPRIdentifier): GEPRTimelineVirtualFile?

  fun findDiffFile(pullRequest: GEPRIdentifier): GEPRDiffVirtualFile?

  fun updateTimelineFilePresentation(details: GiteePullRequest)

  fun addBeforeTimelineFileOpenedListener(disposable: Disposable, listener: (file: GEPRTimelineVirtualFile) -> Unit)
}