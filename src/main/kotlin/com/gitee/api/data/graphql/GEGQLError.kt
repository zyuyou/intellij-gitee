// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data.graphql

import com.intellij.collaboration.api.dto.GraphQLErrorDTO

/**
 * GitHub returns an additional field [type] here contrary to the spec
 */
class GEGQLError(message: String, val type: String?): GraphQLErrorDTO(message) {
  override fun toString(): String = message
}