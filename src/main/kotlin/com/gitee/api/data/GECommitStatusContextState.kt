// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data

enum class GECommitStatusContextState {
  //Status is errored.
  ERROR,

  //Status is expected.
  EXPECTED,

  //Status is failing.
  FAILURE,

  //Status is pending.
  PENDING,

  //Status is successful.
  SUCCESS
}