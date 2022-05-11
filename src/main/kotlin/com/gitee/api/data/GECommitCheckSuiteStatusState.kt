// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data

enum class GECommitCheckSuiteStatusState {
  //The check suite or run has been completed.
  COMPLETED,

  //The check suite or run is in progress.
  IN_PROGRESS,

  //The check suite or run has been queued.
  QUEUED,

  //The check suite or run has been requested.
  REQUESTED
}