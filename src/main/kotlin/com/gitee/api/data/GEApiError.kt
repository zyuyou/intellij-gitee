// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.api.data

/**
 * GitHub returns an additional field [type] here contrary to the spec
 */
class GEApiError(message: String, val type: String?): GEApiErrorDTO(message) {
  override fun toString(): String = message
}