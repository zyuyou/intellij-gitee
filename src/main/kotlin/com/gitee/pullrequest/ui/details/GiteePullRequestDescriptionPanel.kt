// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.details

import com.gitee.api.data.GiteePullRequestDetailed
import com.gitee.ui.util.SingleValueModel
import com.gitee.util.GiteeUtil.Delegates.equalVetoingObservable
import com.intellij.openapi.Disposable
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.HtmlPanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.Font

internal class GiteePullRequestDescriptionPanel(private val model: SingleValueModel<GiteePullRequestDetailed?>) : NonOpaquePanel(), Disposable {

  private var description: String? by equalVetoingObservable<String?>(null) {
    //'!it.isNullOrEmpty()' causes Kotlin compiler to fail here (KT-28847)
    isVisible = it != null && !it.isEmpty()
    htmlPanel.update()
  }

  private val htmlPanel = object : HtmlPanel() {
    init {
      border = JBUI.Borders.empty()
    }

    override fun update() {
      super.update()
      isVisible = !description.isNullOrEmpty()
    }

    override fun getBody() = description.orEmpty()
    override fun getBodyFont(): Font = UIUtil.getLabelFont()
  }

  init {
    setContent(htmlPanel)

    fun update() {
      description = model.value?.bodyHTML
    }

    model.addValueChangedListener(this) {
      update()
    }
    update()
  }

  override fun dispose() {}
}