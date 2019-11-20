// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.graphql

class GiteeGQLSyntaxError(val message: String, val locations: List<Location>) {
  class Location(val line: Int, val column: Int)

  override fun toString(): String = message
}

