// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.pullrequest.ui.details

import com.gitee.api.data.GiteePullRequestDetailed
import com.gitee.pullrequest.data.GiteePRBusyStateTracker
import com.gitee.pullrequest.data.service.GiteePullRequestsSecurityService
import com.gitee.ui.WrapLayout
import com.gitee.ui.util.SingleValueModel
import com.gitee.util.GiteeUtil.Delegates.equalVetoingObservable
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.ui.popup.IconButton
import com.intellij.ui.InplaceButton
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.JBUI.Panels.simplePanel
import com.intellij.util.ui.UIUtil
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.event.ActionListener
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.JLabel

internal abstract class LabeledListPanelHandle<T>(private val model: SingleValueModel<GiteePullRequestDetailed?>,
                                                  private val securityService: GiteePullRequestsSecurityService,
                                                  private val busyStateTracker: GiteePRBusyStateTracker,
                                                  emptyText: String, notEmptyText: String)
  : Disposable {

  val label = JLabel().apply {
    foreground = UIUtil.getContextHelpForeground()
    border = JBUI.Borders.empty(UIUtil.DEFAULT_VGAP + 2, 0, UIUtil.DEFAULT_VGAP + 2, UIUtil.DEFAULT_HGAP / 2)
  }
  val panel = NonOpaquePanel(WrapLayout(FlowLayout.LEADING, 0, 0))

  protected val editButton = InplaceButton(IconButton(null,
                                                      resizeSquareIcon(AllIcons.General.Inline_edit),
                                                      resizeSquareIcon(AllIcons.General.Inline_edit_hovered)),
                                           ActionListener { (::editList)() }).apply {
    cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
    isVisible = securityService.isCurrentUserWithPushAccess()
    isFocusable = true
    addKeyListener(object : KeyAdapter() {
      override fun keyPressed(e: KeyEvent) {
        if (e.keyCode == KeyEvent.VK_ENTER || e.keyCode == KeyEvent.VK_SPACE) {
          doClick()
          e.consume()
        }
      }
    })
  }

  private fun resizeSquareIcon(icon: Icon): Icon {
    val scale = 20f / icon.iconHeight
    return IconUtil.scale(icon, editButton, scale)
  }

  private var list: List<T>? by equalVetoingObservable<List<T>?>(null) { newList ->
    label.text = newList?.let { if (it.isEmpty()) emptyText else notEmptyText }
    label.isVisible = newList != null

    panel.removeAll()
    panel.isVisible = newList != null
    if (newList != null) {
      if (newList.isEmpty()) {
        panel.add(editButton)
      }
      else {
        for (item in newList.dropLast(1)) {
          panel.add(getListItemComponent(item))
        }
        panel.add(getListItemComponent(newList.last(), true))
      }
    }
  }

  init {
    fun update() {
      list = model.value?.let(::extractItems)
      updateButton()
    }

    model.addValueChangedListener(this) {
      update()
    }
    busyStateTracker.addPullRequestBusyStateListener(this) {
      updateButton()
    }
    update()
  }

  private fun updateButton() {
    editButton.isEnabled = !(model.value?.number?.let(busyStateTracker::isBusy) ?: true)
  }

  private fun getListItemComponent(item: T, last: Boolean = false) =
    if (!last) getItemComponent(item)
    else simplePanel(getItemComponent(item)).addToRight(editButton).apply {
      isOpaque = false
    }

  abstract fun extractItems(details: GiteePullRequestDetailed): List<T>?

  abstract fun getItemComponent(item: T): JComponent

  abstract fun editList()

  override fun dispose() {}
}