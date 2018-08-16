// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.authentication.ui

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GiteeAccountInformationProvider
import com.gitee.authentication.accounts.GiteeAccountManager
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.*
import com.intellij.ui.SimpleTextAttributes.STYLE_PLAIN
import com.intellij.ui.SimpleTextAttributes.STYLE_UNDERLINE
import com.intellij.ui.components.JBList
import com.intellij.util.progress.ProgressVisibilityManager
import com.intellij.util.ui.*
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener

private const val ACCOUNT_PICTURE_SIZE: Int = 40
private const val LINK_TAG = "EDIT_LINK"

internal class GiteeAccountsPanel(private val project: Project,
                                  private val executorFactory: GiteeApiRequestExecutor.Factory,
                                  private val accountInformationProvider: GiteeAccountInformationProvider)
  : BorderLayoutPanel(), Disposable {

  private val accountListModel = CollectionListModel<GiteeAccountDecorator>().apply {
    // disable link handler when there are no errors
    addListDataListener(object : ListDataListener {
      override fun contentsChanged(e: ListDataEvent?) = setLinkHandlerEnabled(items.any { it.loadingError != null })
      override fun intervalRemoved(e: ListDataEvent?) {}
      override fun intervalAdded(e: ListDataEvent?) {}
    })
  }

  private val accountList = JBList<GiteeAccountDecorator>(accountListModel).apply {
    cellRenderer = GiteeAccountDecoratorRenderer()

    selectionMode = ListSelectionModel.SINGLE_SELECTION
    selectionForeground = UIUtil.getListForeground()
    selectionBackground = JBColor(0xE9EEF5, 0x464A4D)

    emptyText.apply {
      appendText("No Gitee accounts added.")
      appendSecondaryText("Add account", SimpleTextAttributes.LINK_ATTRIBUTES) { addAccount() }
      appendSecondaryText(" (${KeymapUtil.getFirstKeyboardShortcutText(CommonShortcuts.getNew())})", StatusText.DEFAULT_ATTRIBUTES, null)
    }
  }

  private val progressManager = createListProgressManager()
  private val errorLinkHandler = createLinkActivationListener()
  private var errorLinkHandlerInstalled = false
  private var currentTokensMap = mapOf<com.gitee.authentication.accounts.GiteeAccount, Pair<String, String>?>()
  private val newTokensMap = mutableMapOf<com.gitee.authentication.accounts.GiteeAccount, Pair<String, String>>()

  init {
    addToCenter(ToolbarDecorator.createDecorator(accountList)
      .disableUpDownActions()
      .setPanelBorder(IdeBorderFactory.createBorder(SideBorder.TOP or SideBorder.BOTTOM))
      .setAddAction { addAccount() }
      .addExtraAction(object : ToolbarDecorator.ElementActionButton("Set default", AllIcons.Actions.Checked) {

        override fun actionPerformed(e: AnActionEvent) {
          if (accountList.selectedValue.projectDefault) return

          for (accountData in accountListModel.items) {
            if (accountData == accountList.selectedValue) {
              accountData.projectDefault = true
              accountListModel.contentsChanged(accountData)
            } else if (accountData.projectDefault) {
              accountData.projectDefault = false
              accountListModel.contentsChanged(accountData)
            }
          }
        }

        override fun updateButton(e: AnActionEvent) {
          isEnabled = isEnabled && !accountList.selectedValue.projectDefault
        }
      })
      .createPanel())

    Disposer.register(this, progressManager)
  }

  private fun addAccount() {
    val dialog = GiteeLoginDialog(executorFactory, project, this, ::isAccountUnique)

    if (dialog.showAndGet()) {
      val githubAccount = GiteeAccountManager.createAccount(dialog.getLogin(), dialog.getServer())
      newTokensMap[githubAccount] = dialog.getAccessToken() to dialog.getRefreshToken()

      val accountData = GiteeAccountDecorator(githubAccount, false)
      accountListModel.add(accountData)

      loadAccountDetails(accountData)
    }
  }

  private fun editAccount(decorator: GiteeAccountDecorator) {
    val dialog = GiteeLoginDialog(executorFactory, project, this).apply {
      withServer(decorator.account.server.toString(), false)
      withCredentials(decorator.account.name)
    }

    if (dialog.showAndGet()) {
      decorator.account.name = dialog.getLogin()

      newTokensMap[decorator.account] = dialog.getAccessToken() to dialog.getRefreshToken()

      loadAccountDetails(decorator)
    }
  }

  private fun isAccountUnique(login: String, server: GiteeServerPath) =
    accountListModel.items.none { it.account.name == login && it.account.server == server }

  /**
   * Manages link hover and click for [GiteeAccountDecoratorRenderer.loadingError]
   * Sets the proper cursor and underlines the link on hover
   *
   * @see [GiteeAccountDecorator.loadingError]
   * @see [GiteeAccountDecorator.showLoginLink]
   * @see [GiteeAccountDecorator.errorLinkPointedAt]
   */
  private fun createLinkActivationListener() = object : MouseAdapter() {

    override fun mouseMoved(e: MouseEvent) {
      val decorator = findDecoratorWithLoginLinkAt(e.point)
      if (decorator != null) {
        UIUtil.setCursor(accountList, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
      } else {
        UIUtil.setCursor(accountList, Cursor.getDefaultCursor())
      }

      var hasChanges = false
      for (item in accountListModel.items) {
        val isLinkPointedAt = item == decorator
        hasChanges = hasChanges || isLinkPointedAt != item.errorLinkPointedAt
        item.errorLinkPointedAt = isLinkPointedAt
      }
      if (hasChanges) accountListModel.allContentsChanged()
    }

    override fun mouseClicked(e: MouseEvent) {
      findDecoratorWithLoginLinkAt(e.point)?.run(::editAccount)
    }

    /**
     * Checks if mouse is pointed at decorator error link
     *
     * @return decorator with error link under mouse pointer or null
     */
    private fun findDecoratorWithLoginLinkAt(point: Point): GiteeAccountDecorator? {
      val idx = accountList.locationToIndex(point)
      if (idx < 0) return null

      val cellBounds = accountList.getCellBounds(idx, idx)
      if (!cellBounds.contains(point)) return null

      val decorator = accountListModel.getElementAt(idx)
      if (decorator?.loadingError == null) return null

      val rendererComponent = accountList.cellRenderer.getListCellRendererComponent(accountList, decorator, idx, true, true)
      rendererComponent.setBounds(cellBounds.x, cellBounds.y, cellBounds.width, cellBounds.height)
      layoutRecursively(rendererComponent)

      val rendererRelativeX = point.x - cellBounds.x
      val rendererRelativeY = point.y - cellBounds.y
      val childComponent = UIUtil.getDeepestComponentAt(rendererComponent, rendererRelativeX, rendererRelativeY)
      if (childComponent !is SimpleColoredComponent) return null

      val childRelativeX = rendererRelativeX - childComponent.parent.x - childComponent.x
      return if (childComponent.getFragmentTagAt(childRelativeX) == LINK_TAG) decorator else null
    }
  }

  private fun setLinkHandlerEnabled(enabled: Boolean) {
    if (enabled) {
      if (!errorLinkHandlerInstalled) {
        accountList.addMouseListener(errorLinkHandler)
        accountList.addMouseMotionListener(errorLinkHandler)
        errorLinkHandlerInstalled = true
      }
    } else if (errorLinkHandlerInstalled) {
      accountList.removeMouseListener(errorLinkHandler)
      accountList.removeMouseMotionListener(errorLinkHandler)
      errorLinkHandlerInstalled = false
    }
  }

  fun loadExistingAccountsDetails() {
    for (accountData in accountListModel.items) {
      loadAccountDetails(accountData)
    }
  }

  private fun loadAccountDetails(accountData: GiteeAccountDecorator) {
    val account = accountData.account
//    val token = newTokensMap[account] ?: currentTokensMap[account]
    val tokens = newTokensMap[account] ?: currentTokensMap[account]

    if (tokens == null) {
      accountListModel.contentsChanged(accountData.apply {
        loadingError = "Missing access token"
        showLoginLink = true
      })
      return
    }

    progressManager.run(object : Task.Backgroundable(project, "Not Visible") {
      lateinit var data: Pair<com.gitee.api.data.GiteeUserDetailed, Image?>

      override fun run(indicator: ProgressIndicator) {
//        val executor = executorFactory.create(tokens, Supplier {
//          refreshAccount(accountData, tokens.second, executorFactory.create(), indicator)
//        })

        val executor = executorFactory.create(tokens) {
          project.service<GiteeAuthenticationManager>().refreshNewTokens(accountData.account, it)
        }

        val details = accountInformationProvider.getInformation(executor, indicator, account)

        val image = details.avatarUrl?.let {
          accountInformationProvider.getAvatar(executor, indicator, account, it)
        }
        data = details to image
      }

      override fun onSuccess() {
        accountListModel.contentsChanged(accountData.apply {
          fullName = data.first.name
          profilePicture = data.second
          loadingError = null
          showLoginLink = false
        })
      }

      override fun onThrowable(error: Throwable) {
        accountListModel.contentsChanged(accountData.apply {
          loadingError = error.message.toString()
          showLoginLink = error is com.gitee.exceptions.GiteeAuthenticationException
        })
      }
    })
  }

  private fun createListProgressManager() = object : ProgressVisibilityManager() {
    override fun setProgressVisible(visible: Boolean) = accountList.setPaintBusy(visible)
    override fun getModalityState() = ModalityState.any()
  }

  fun setAccounts(accounts: Map<com.gitee.authentication.accounts.GiteeAccount, Pair<String, String>?>, defaultAccount: com.gitee.authentication.accounts.GiteeAccount?) {
    accountListModel.removeAll()
    accountListModel.addAll(0, accounts.keys.map { GiteeAccountDecorator(it, it == defaultAccount) })
    currentTokensMap = accounts
  }

  /**
   * @return list of accounts and associated tokens if new token was created and selected default account
   */
  fun getAccounts(): Pair<Map<com.gitee.authentication.accounts.GiteeAccount, Pair<String, String>?>, com.gitee.authentication.accounts.GiteeAccount?> {
    return accountListModel.items.associate { it.account to newTokensMap[it.account] } to
      accountListModel.items.find { it.projectDefault }?.account
  }

  fun clearNewTokens() = newTokensMap.clear()

  fun isModified(accounts: Set<com.gitee.authentication.accounts.GiteeAccount>, defaultAccount: com.gitee.authentication.accounts.GiteeAccount?): Boolean {
    return accountListModel.items.find { it.projectDefault }?.account != defaultAccount
      || accountListModel.items.map { it.account }.toSet() != accounts
      || newTokensMap.isNotEmpty()
  }

  override fun dispose() {}

  companion object {
    private fun layoutRecursively(component: Component) {
      if (component is JComponent) {
        component.doLayout()
        for (child in component.components) {
          layoutRecursively(child)
        }
      }
    }
  }
}

private class GiteeAccountDecoratorRenderer : ListCellRenderer<GiteeAccountDecorator>, JPanel() {
  private val accountName = JLabel()

  private val serverName = JLabel()
  private val profilePicture = JLabel()

  private val fullName = JLabel()

  private val loadingError = SimpleColoredComponent()

  /**
   * UPDATE [createLinkActivationListener] IF YOU CHANGE LAYOUT
   */
  init {
    layout = FlowLayout(FlowLayout.LEFT, 0, 0)
    border = JBUI.Borders.empty(5, 8)

    val namesPanel = JPanel().apply {
      layout = GridBagLayout()
      border = JBUI.Borders.empty(0, 6, 4, 6)

      val bag = GridBag()
        .setDefaultInsets(JBUI.insetsRight(UIUtil.DEFAULT_HGAP))
        .setDefaultAnchor(GridBagConstraints.WEST)
        .setDefaultFill(GridBagConstraints.VERTICAL)
      add(fullName, bag.nextLine().next())
      add(accountName, bag.next())
      add(loadingError, bag.next())
      add(serverName, bag.nextLine().coverLine())
    }

    add(profilePicture)
    add(namesPanel)
  }

  override fun getListCellRendererComponent(list: JList<out GiteeAccountDecorator>,
                                            value: GiteeAccountDecorator,
                                            index: Int,
                                            isSelected: Boolean,
                                            cellHasFocus: Boolean): Component {
    UIUtil.setBackgroundRecursively(this, if (isSelected) list.selectionBackground else list.background)

    val textColor = if (isSelected) list.selectionForeground else list.foreground
    val grayTextColor = if (isSelected) list.selectionForeground else Gray._120

    accountName.apply {
      text = value.account.name
      setBold(if (value.fullName == null) value.projectDefault else false)
      foreground = if (value.fullName == null) textColor else grayTextColor
    }

    serverName.apply {
      text = value.account.server.toString()
      foreground = grayTextColor
    }

    profilePicture.apply {
      icon = value.profilePicture?.let {
        val size = JBUI.scale(ACCOUNT_PICTURE_SIZE)
        JBImageIcon(it.getScaledInstance(size, size, java.awt.Image.SCALE_FAST))
      } ?: com.gitee.icons.GiteeIcons.DefaultAvatar_40
    }

    fullName.apply {
      text = value.fullName
      setBold(value.projectDefault)
      isVisible = value.fullName != null
      foreground = textColor
    }

    loadingError.apply {
      clear()
      value.loadingError?.let {

        append(it, SimpleTextAttributes.ERROR_ATTRIBUTES)
        append(" ")

        if (value.showLoginLink) append("Log In",
          if (value.errorLinkPointedAt) SimpleTextAttributes(STYLE_UNDERLINE, JBColor.link())
          else SimpleTextAttributes(STYLE_PLAIN, JBColor.link()),
          LINK_TAG)
      }
    }

    return this
  }

  companion object {
    private fun JLabel.setBold(isBold: Boolean) {
      font = font.deriveFont(if (isBold) font.style or Font.BOLD else font.style and Font.BOLD.inv())
    }
  }
}

/**
 * Account + auxillary info + info loading error
 */
private class GiteeAccountDecorator(val account: com.gitee.authentication.accounts.GiteeAccount, var projectDefault: Boolean) {
  var fullName: String? = null
  var profilePicture: Image? = null
  var loadingError: String? = null

  var showLoginLink = false
  var errorLinkPointedAt = false

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as GiteeAccountDecorator

    if (account != other.account) return false

    return true
  }

  override fun hashCode(): Int {
    return account.hashCode()
  }
}
