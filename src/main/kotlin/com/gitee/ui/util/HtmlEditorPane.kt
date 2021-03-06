// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.ui.util

import com.gitee.icons.GiteeIcons
import com.intellij.ide.ui.UISettings
import com.intellij.openapi.util.IconLoader
import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.util.ui.JBHtmlEditorKit
import com.intellij.util.ui.JBUI
import java.awt.Graphics
import java.awt.Shape
import javax.swing.JEditorPane
import javax.swing.text.DefaultCaret
import javax.swing.text.Element
import javax.swing.text.View
import javax.swing.text.ViewFactory
import javax.swing.text.html.HTML
import javax.swing.text.html.InlineView

internal class HtmlEditorPane() : JEditorPane() {
  constructor(body: String) : this() {
    setBody(body)
  }

  init {
    editorKit = object : JBHtmlEditorKit(true) {
      override fun getViewFactory(): ViewFactory {
        return object : JBHtmlFactory() {
          override fun create(elem: Element): View {
            if ("icon-inline" == elem.name) {
              val icon = elem.attributes.getAttribute(HTML.Attribute.SRC)
                ?.let { IconLoader.getIcon(it as String, GiteeIcons::class.java) }

              if (icon != null) {
                return object : InlineView(elem) {

                  override fun getPreferredSpan(axis: Int): Float {
                    when (axis) {
                      View.X_AXIS -> return icon.iconWidth.toFloat() + super.getPreferredSpan(axis)
                      else -> return super.getPreferredSpan(axis)
                    }
                  }

                  override fun paint(g: Graphics, allocation: Shape) {
                    super.paint(g, allocation)
                    icon.paintIcon(null, g, allocation.bounds.x, allocation.bounds.y)
                  }
                }
              }
            }
            return super.create(elem)
          }
        }
      }
    }

    isEditable = false
    isOpaque = false
    addHyperlinkListener(BrowserHyperlinkListener.INSTANCE)
    margin = JBUI.emptyInsets()

    val caret = caret as DefaultCaret
    caret.updatePolicy = DefaultCaret.NEVER_UPDATE
  }

  fun setBody(body: String) {
    if (body.isEmpty()) {
      text = ""
    }
    else {
      text = "<html><body>$body</body></html>"
    }
  }

  override fun updateUI() {
    super.updateUI()
    UISettings.setupComponentAntialiasing(this)
  }
}