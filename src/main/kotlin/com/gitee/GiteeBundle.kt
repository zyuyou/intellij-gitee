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

package com.gitee

import com.intellij.AbstractBundle
import org.jetbrains.annotations.PropertyKey

import java.io.UnsupportedEncodingException
import java.nio.charset.StandardCharsets

/**
 * @author Yuyou Chow
 */
private const val PATH_TO_BUNDLE = "i18n.GiteeBundle"

class GiteeBundle : AbstractBundle(PATH_TO_BUNDLE) {
  companion object {
//    const val PATH_TO_BUNDLE = "i18n.GiteeBundle"
    private val BUNDLE = GiteeBundle()

    @JvmStatic
    fun message(@PropertyKey(resourceBundle = PATH_TO_BUNDLE) key: String, vararg params: Any): String {
      return BUNDLE.getMessage(key, *params)
    }

    @JvmStatic
    fun message2(@PropertyKey(resourceBundle = PATH_TO_BUNDLE) key: String, vararg params: Any): String {
      val msg = message(key, *params)
      try {
        return String(msg.toByteArray(charset("ISO8859-1")), StandardCharsets.UTF_8)
      } catch (ignore: UnsupportedEncodingException) {
      }

      return msg
    }
  }
}
