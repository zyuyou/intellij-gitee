// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.gitee.ui.component

import com.gitee.api.GiteeApiRequestExecutorManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.accounts.GiteeAccountInformationProvider
import com.gitee.i18n.GiteeBundle
import com.gitee.icons.GiteeIcons
import com.gitee.ui.util.GEUIUtil
import com.gitee.ui.util.getName
import com.gitee.util.CachingGEUserAvatarLoader
import com.intellij.collaboration.async.CompletableFutureUtil.submitIOTask
import com.intellij.collaboration.ui.codereview.avatar.CachingAvatarIconsProvider
import com.intellij.ide.ui.laf.darcula.DarculaUIUtil
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupListener
import com.intellij.openapi.ui.popup.LightweightWindowEvent
import com.intellij.ui.popup.list.ComboBoxPopup
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.JBInsets
import com.intellij.util.ui.UIUtil
import com.intellij.util.ui.cloneDialog.AccountMenuItem
import com.intellij.util.ui.cloneDialog.AccountMenuItemRenderer
import com.intellij.util.ui.cloneDialog.VcsCloneDialogUiSpec
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.concurrent.CompletableFuture
import javax.swing.*
import javax.swing.border.Border
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener

class GEAccountSelectorComponentFactory {

  fun create(model: ComboBoxWithActionsModel<GiteeAccount>): JComponent {
    val label = JLabel().apply {
      isOpaque = false
      cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
      isFocusable = true
      border = FocusBorder()
    }
    Controller(model, label)
    return label
  }

  class FocusBorder : Border {
    override fun paintBorder(c: Component?, g: Graphics?, x: Int, y: Int, width: Int, height: Int) {
      if (c?.hasFocus() == true && g is Graphics2D) {
        DarculaUIUtil.paintFocusBorder(g, width, height, 0f, true)
      }
    }

    override fun getBorderInsets(c: Component): Insets {
      val g2d = c.graphics as? Graphics2D ?: return JBInsets.emptyInsets()

      val bw = if (UIUtil.isUnderDefaultMacTheme()) JBUIScale.scale(3).toFloat() else DarculaUIUtil.BW.float
      val f = if (UIUtil.isRetina(g2d)) 0.5f else 1.0f
      val lw = if (UIUtil.isUnderDefaultMacTheme()) JBUIScale.scale(f) else DarculaUIUtil.LW.float
      val insets = (bw + lw).toInt()
      return Insets(insets, insets, insets, insets)
    }

    override fun isBorderOpaque() = false
  }

  class Controller(private val accountsModel: ComboBoxWithActionsModel<GiteeAccount>, private val label: JLabel)
    : ComboBoxPopup.Context<ComboBoxWithActionsModel.Item<GiteeAccount>> {

    private val avatarsProvider = AccountAvatarIconsProvider()
    private var popup: ComboBoxPopup<*>? = null

    init {
      accountsModel.addListDataListener(object : ListDataListener {
        override fun contentsChanged(e: ListDataEvent) = updateLabel()
        override fun intervalAdded(e: ListDataEvent?) = updateLabel()
        override fun intervalRemoved(e: ListDataEvent?) = updateLabel()
      })
      label.addMouseListener(object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent?) = showPopup()
      })
      label.registerPopupOnKeyboardShortcut(KeyEvent.VK_ENTER)
      label.registerPopupOnKeyboardShortcut(KeyEvent.VK_SPACE)
      updateLabel()
    }

    private fun updateLabel() {
      val selectedAccount = accountsModel.selectedItem?.wrappee
      with(label) {
        isVisible = accountsModel.items.isNotEmpty()

        icon = avatarsProvider.getIcon(selectedAccount, GEUIUtil.AVATAR_SIZE)
        toolTipText = selectedAccount?.name ?: GiteeBundle.message("account.choose.link")
      }
    }

    private fun showPopup() {
      popup = object : ComboBoxPopup<ComboBoxWithActionsModel.Item<GiteeAccount>>(this, accountsModel.selectedItem, {
        accountsModel.setSelectedItem(it)
      }) {
        //otherwise component borders are overridden
        override fun getListElementRenderer() = renderer
      }.apply {
        //TODO: remove speedsearch
        addListener(object : JBPopupListener {
          override fun onClosed(event: LightweightWindowEvent) {
            popup = null
          }
        })
        showUnderneathOf(label)
      }
    }

    private fun JComponent.registerPopupOnKeyboardShortcut(keyCode: Int) {
      registerKeyboardAction({ showPopup() }, KeyStroke.getKeyStroke(keyCode, 0), JComponent.WHEN_FOCUSED)
    }

    override fun getProject(): Project? = null

    override fun getModel(): ListModel<ComboBoxWithActionsModel.Item<GiteeAccount>> = accountsModel

    override fun getRenderer(): ListCellRenderer<ComboBoxWithActionsModel.Item<GiteeAccount>> = PopupItemRenderer(avatarsProvider)
  }

  private class PopupItemRenderer(private val avatarsProvider: AccountAvatarIconsProvider)
    : ListCellRenderer<ComboBoxWithActionsModel.Item<GiteeAccount>> {

    private val delegateRenderer = AccountMenuItemRenderer()

    override fun getListCellRendererComponent(list: JList<out ComboBoxWithActionsModel.Item<GiteeAccount>>?,
                                              value: ComboBoxWithActionsModel.Item<GiteeAccount>,
                                              index: Int,
                                              selected: Boolean,
                                              focused: Boolean): Component {
      val item = when (value) {
        is ComboBoxWithActionsModel.Item.Wrapper<GiteeAccount> ->
          value.wrappee.let { account ->
            val icon = avatarsProvider.getIcon(account, VcsCloneDialogUiSpec.Components.popupMenuAvatarSize)
            val serverAddress = account.server.toString()
            AccountMenuItem.Account(account.name, serverAddress, icon)
          }
        is ComboBoxWithActionsModel.Item.Action<GiteeAccount> ->
          value.action.let {
            AccountMenuItem.Action(it.getName(), {}, showSeparatorAbove = value.needSeparatorAbove)
          }
      }
      return delegateRenderer.getListCellRendererComponent(null, item, index, selected, focused)
    }
  }

  private class AccountAvatarIconsProvider : CachingAvatarIconsProvider<GiteeAccount>(
    GiteeIcons.DefaultAvatar) {

    private val requestExecutorManager = GiteeApiRequestExecutorManager.getInstance()
    private val accountInformationProvider = GiteeAccountInformationProvider.getInstance()
    private val avatarLoader = CachingGEUserAvatarLoader.getInstance()

    override fun loadImage(key: GiteeAccount): Image? {
      val executor = requestExecutorManager.getExecutor(key)
      return ProgressManager.getInstance().submitIOTask(EmptyProgressIndicator()) {
        accountInformationProvider.getInformation(executor, it, key)
      }.thenCompose {
        val url = it.avatarUrl ?: return@thenCompose CompletableFuture.completedFuture(null)
        avatarLoader.requestAvatar(executor, url)
      }.get()
    }
  }
}