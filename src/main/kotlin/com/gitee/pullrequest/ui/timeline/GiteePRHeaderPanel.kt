// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.timeline

import com.gitee.api.data.GiteePullRequest
import com.gitee.api.data.pullrequest.GEPullRequest
import com.gitee.pullrequest.avatars.CachingGiteeAvatarIconsProvider
import com.gitee.ui.util.HtmlEditorPane
import com.gitee.ui.util.SingleValueModel
import com.gitee.util.GiteeUIUtil
import com.intellij.ide.BrowserUtil
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.labels.LinkListener
import com.intellij.util.ui.JBDimension
import com.intellij.util.ui.UI
import com.intellij.util.ui.UIUtil
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import javax.swing.Box
import javax.swing.JPanel

internal class GiteePRHeaderPanel(private val model: SingleValueModel<GiteePullRequest>,
                                  avatarIconsProvider: CachingGiteeAvatarIconsProvider)
  : JPanel() {

  private val authorAvatar = LinkLabel<Any>("", avatarIconsProvider.getIcon(model.value.user?.avatarUrl), LinkListener { _, _ ->
    model.value.user?.htmlUrl?.let { BrowserUtil.browse(it) }
  })

  //language=html
  private val createText = HtmlEditorPane("<a href='${model.value.user?.htmlUrl}'>${model.value.user?.login ?: "unknown"}</a> " +
                                          "created ${GiteeUIUtil.formatActionDate(model.value.createdAt)}").apply {
    foreground = UIUtil.getContextHelpForeground()
  }

  private val title = JBLabel(UIUtil.ComponentStyle.LARGE).apply {
    font = font.deriveFont((font.size * 1.5).toFloat())
  }

  private val number = JBLabel(UIUtil.ComponentStyle.LARGE).apply {
    font = font.deriveFont((font.size * 1.4).toFloat())
    foreground = UIUtil.getContextHelpForeground()
  }

  private val descriptionPane = HtmlEditorPane()

  init {
    isOpaque = false
    layout = MigLayout(LC().gridGap("0", "0")
                         .insets("0", "0", "0", "0")
                         .fill()
                         .noGrid()).apply {
      rowConstraints = "[]${UI.scale(4)}[]${UI.scale(8)}[]"
    }

    add(authorAvatar, noGap())
    add(Box.createRigidArea(JBDimension(5, 0)), noGap())
    add(createText, noGap().wrap())

    add(title, noGap())
    add(Box.createRigidArea(JBDimension(10, 0)), noGap())
    add(number, noGap().wrap())

    add(descriptionPane, CC().grow().push().minWidth("0"))

    fun update() {
      title.text = model.value.title
      number.text = "#" + model.value.number
      descriptionPane.setBody((model.value as? GEPullRequest)?.bodyHTML.orEmpty())
    }

    model.addValueChangedListener {
      update()
    }
    update()
  }

  companion object {
    private fun noGap() = CC().gap("0", "0", "0", "0")
  }
}