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
package com.gitee.util

import com.intellij.collaboration.ui.SimpleEventListener
import com.intellij.concurrency.JobScheduler
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.Couple
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.EventDispatcher
import java.io.IOException
import java.net.UnknownHostException
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import kotlin.properties.ObservableProperty
import kotlin.reflect.KProperty

/**
 * Various utility methods for the GutHub plugin.
 *
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/util/GiteeUtil.java
 * @author JetBrains s.r.o.
 */
object GiteeUtil {

  @JvmField
  val LOG: Logger = Logger.getInstance("gitee")

  const val SERVICE_DISPLAY_NAME: String = "Gitee"
  const val ENTERPRISE_SERVICE_DISPLAY_NAME: String = "Gitee Enterprise"
  const val GIT_AUTH_PASSWORD_SUBSTITUTE: String = "x-oauth-basic"

  @JvmStatic
  fun addCancellationListener(run: () -> Unit): ScheduledFuture<*> {
    return JobScheduler.getScheduler().scheduleWithFixedDelay(run, 1000, 300, TimeUnit.MILLISECONDS)
  }

  private fun addCancellationListener(indicator: ProgressIndicator, thread: Thread): ScheduledFuture<*> {
    return addCancellationListener { if (indicator.isCanceled) thread.interrupt() }
  }

  @Throws(IOException::class)
  @JvmStatic
  fun <T> runInterruptable(indicator: ProgressIndicator,
                           task: ThrowableComputable<T, IOException>): T {
    var future: ScheduledFuture<*>? = null

    try {
      val thread = Thread.currentThread()
      future = addCancellationListener(indicator, thread)

      return task.compute()
    } finally {
      future?.cancel(true)
      Thread.interrupted()
    }
  }

  @JvmStatic
  fun getErrorTextFromException(e: Throwable): String {
    return if (e is UnknownHostException) {
      "Unknown host: " + e.message
    } else {
      StringUtil.notNullize(e.message, "Unknown error")
    }
  }

  /**
   * Splits full commit message into subject and description in Gitee style:
   * First line becomes subject, everything after first line becomes description
   * Also supports empty line that separates subject and description
   *
   * @param commitMessage full commit message
   * @return couple of subject and description based on full commit message
   */
  @JvmStatic
  fun getGiteeLikeFormattedDescriptionMessage(commitMessage: String?): Couple<String> {
    // Trim original
    val message = commitMessage?.trim { it <= ' ' } ?: ""
    if (message.isEmpty()) {
      return Couple.of("", "")
    }
    val firstLineEnd = message.indexOf("\n")
    val subject: String
    val description: String
    if (firstLineEnd > -1) {
      // Subject is always first line
      subject = message.substring(0, firstLineEnd).trim { it <= ' ' }
      // Description is all text after first line, we also trim it to remove empty lines on start of description
      description = message.substring(firstLineEnd + 1).trim { it <= ' ' }
    } else {
      // If we don't have any line separators and cannot detect description,
      // we just assume that it is one-line commit and use full message as subject with empty description
      subject = message
      description = ""
    }

    return Couple.of(subject, description)
  }

  object Delegates {
    inline fun <T> equalVetoingObservable(initialValue: T, crossinline onChange: (newValue: T) -> Unit) =
      object : ObservableProperty<T>(initialValue) {
        override fun beforeChange(property: KProperty<*>, oldValue: T, newValue: T) = newValue == null || oldValue != newValue
        override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) = onChange(newValue)
      }

    fun <T> observableField(initialValue: T, dispatcher: EventDispatcher<SimpleEventListener>): ObservableProperty<T> {
      return object : ObservableProperty<T>(initialValue) {
        override fun afterChange(property: KProperty<*>, oldValue: T, newValue: T) = dispatcher.multicaster.eventOccurred()
      }
    }
  }
}
