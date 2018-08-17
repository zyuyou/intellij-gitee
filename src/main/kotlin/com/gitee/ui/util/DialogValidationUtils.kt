/*
 * Copyright 2016-2018 码云 - Gitee
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.gitee.ui.util

import com.intellij.openapi.ui.ValidationInfo
import javax.swing.JTextField

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/ui/util/DialogValidationUtils.kt
 * @author JetBrains s.r.o.
 */
object DialogValidationUtils {
  /**
   * Returns [ValidationInfo] with [message] if [textField] is blank
   */
  fun notBlank(textField: JTextField, message: String): ValidationInfo? {
    return if (textField.text.isNullOrBlank()) ValidationInfo(message, textField) else null
  }

  /**
   * Chains the [validators] so that if one of them returns non-null [ValidationInfo] the rest of them are not checked
   */
  fun chain(vararg validators: Validator): Validator = { validators.asSequence().mapNotNull { it() }.firstOrNull() }

  /**
   * Stateful validator that checks that contents of [textField] are unique among [records]
   */
  class RecordUniqueValidator(private val textField: JTextField, private val message: String) : Validator {
    var records: Set<String> = setOf<String>()

    override fun invoke(): ValidationInfo? = if (records.contains(textField.text)) ValidationInfo(message, textField) else null
  }
}

typealias Validator = () -> ValidationInfo?