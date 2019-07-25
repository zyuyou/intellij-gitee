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
package com.gitee.pullrequest.ui

import com.intellij.ui.components.panels.Wrapper
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Cursor
import javax.swing.JEditorPane
import javax.swing.event.HyperlinkEvent
import javax.swing.text.html.HTMLEditorKit

/**
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/pullrequest/ui/HtmlErrorPanel.kt
 * @author JetBrains s.r.o.
 */
class HtmlErrorPanel : Wrapper() {
  private var currentSeverity: Severity? = null
  private var currentLinkActivationListener: ((HyperlinkEvent) -> Unit)? = null

  private val errorPane = JEditorPane().apply {
    editorKit = UIUtil.getHTMLEditorKit()
    val linkColor = JBUI.CurrentTheme.Link.linkColor()
    //language=CSS
    (editorKit as HTMLEditorKit).styleSheet.addRule("a {color: rgb(${linkColor.red}, ${linkColor.green}, ${linkColor.blue})}")
    addHyperlinkListener { e ->
      if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
        currentLinkActivationListener?.invoke(e)
      }
      else {
        cursor = if (e.eventType == HyperlinkEvent.EventType.ENTERED) Cursor(Cursor.HAND_CURSOR)
        else Cursor(Cursor.DEFAULT_CURSOR)
      }
    }
    isEditable = false
    isFocusable = false
    isOpaque = false
    border = JBUI.Borders.empty(UIUtil.DEFAULT_VGAP, UIUtil.DEFAULT_HGAP)
  }

  init {
    setContent(errorPane)
    isOpaque = true
    isVisible = false
  }

  fun setError(errorText: String?) {
    if (errorText == null) {
      currentSeverity = null
      currentLinkActivationListener = null
      errorPane.text = ""
      isVisible = false
    }
    else setError(errorText)
  }

  fun setError(errorText: String, severity: Severity = Severity.ERROR, linkActivationListener: ((HyperlinkEvent) -> Unit)? = null) {
    val currentSevPriority = currentSeverity?.ordinal
    if (currentSevPriority != null && currentSevPriority > severity.ordinal) return

    errorPane.text = errorText
    currentSeverity = severity
    currentLinkActivationListener = linkActivationListener
    background = when (severity) {
      Severity.ERROR -> JBUI.CurrentTheme.Validator.errorBackgroundColor()
      Severity.WARNING -> JBUI.CurrentTheme.Validator.warningBackgroundColor()
    }
    isVisible = true
  }

  enum class Severity {
    WARNING, ERROR
  }
}