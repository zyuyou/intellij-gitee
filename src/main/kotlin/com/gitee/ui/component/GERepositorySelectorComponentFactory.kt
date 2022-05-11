// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.ui.component

import com.gitee.ui.util.GEUIUtil
import com.gitee.ui.util.getName
import com.gitee.util.GEGitRepositoryMapping
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.SideBorder
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.castSafelyTo
import javax.swing.JList

class GERepositorySelectorComponentFactory {
  fun create(model: ComboBoxWithActionsModel<GEGitRepositoryMapping>): ComboBox<*> {
    return ComboBox(model).apply {
      renderer = object : ColoredListCellRenderer<ComboBoxWithActionsModel.Item<GEGitRepositoryMapping>>() {
        override fun customizeCellRenderer(list: JList<out ComboBoxWithActionsModel.Item<GEGitRepositoryMapping>>,
                                           value: ComboBoxWithActionsModel.Item<GEGitRepositoryMapping>?,
                                           index: Int,
                                           selected: Boolean,
                                           hasFocus: Boolean) {
          if (value is ComboBoxWithActionsModel.Item.Wrapper) {
            val mapping = value.wrappee.castSafelyTo<GEGitRepositoryMapping>() ?: return
            val repositoryName = GEUIUtil.getRepositoryDisplayName(model.items.map(GEGitRepositoryMapping::geRepositoryCoordinates),
                                                                   mapping.geRepositoryCoordinates,
                                                                   true)
            val remoteName = mapping.gitRemoteUrlCoordinates.remote.name
            append(repositoryName).append(" ").append(remoteName, SimpleTextAttributes.GRAYED_ATTRIBUTES)
          }
          if (value is ComboBoxWithActionsModel.Item.Action) {
            if (model.size == index) border = IdeBorderFactory.createBorder(SideBorder.TOP)
            append(value.action.getName())
          }
        }
      }
      isUsePreferredSizeAsMinimum = false
      isOpaque = false
      isSwingPopup = true
    }
  }
}