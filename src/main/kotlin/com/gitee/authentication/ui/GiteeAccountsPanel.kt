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
package com.gitee.authentication.ui

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequests
import com.gitee.api.GiteeServerPath
import com.gitee.api.data.GiteeAuthenticatedUser
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.accounts.GiteeAccountManager
import com.gitee.exceptions.GiteeAuthenticationException
import com.gitee.i18n.GiteeBundle
import com.gitee.pullrequest.avatars.CachingGiteeAvatarIconsProvider
import com.gitee.pullrequest.avatars.GiteeAvatarIconsProvider
import com.gitee.ui.util.JListHoveredRowMaterialiser
import com.gitee.util.CachingGiteeUserAvatarLoader
import com.gitee.util.GiteeImageResizer
import com.gitee.util.GiteeUIUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.keymap.KeymapUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.CollectionListModel
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBList
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.ui.components.labels.LinkListener
import com.intellij.util.progress.ProgressVisibilityManager
import com.intellij.util.ui.*
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.*
import javax.swing.*

private val actionManager: ActionManager get() = ActionManager.getInstance()

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/authentication/ui/GithubAccountsPanel.kt
 * @author JetBrains s.r.o.
 */
internal class GiteeAccountsPanel(
  private val project: Project,
  private val executorFactory: GiteeApiRequestExecutor.Factory,
  private val avatarLoader: CachingGiteeUserAvatarLoader,
  private val imageResizer: GiteeImageResizer
) : BorderLayoutPanel(), Disposable, DataProvider {

  companion object {
    val KEY: DataKey<GiteeAccountsPanel> = DataKey.create("Gitee.AccountsPanel")
  }

  private val accountListModel = CollectionListModel<GiteeAccountDecorator>()

  private val accountList = JBList<GiteeAccountDecorator>(accountListModel).apply {
    val decoratorRenderer = GiteeAccountDecoratorRenderer()
    cellRenderer = decoratorRenderer
    JListHoveredRowMaterialiser.install(this, GiteeAccountDecoratorRenderer())
    UIUtil.putClientProperty(this, UIUtil.NOT_IN_HIERARCHY_COMPONENTS, listOf(decoratorRenderer))

    selectionMode = ListSelectionModel.SINGLE_SELECTION
  }

  private val progressManager = createListProgressManager()

  private var currentTokensMap = mapOf<GiteeAccount, Pair<String, String>?>()
  private val newTokensMap = mutableMapOf<GiteeAccount, Pair<String, String>>()

  init {
    accountList.emptyText.apply {
      appendText(GiteeBundle.message("accounts.none.added"))
      appendSecondaryText(GiteeBundle.message("accounts.add"), SimpleTextAttributes.LINK_PLAIN_ATTRIBUTES) {
        ActionUtil.invokeAction(actionManager.getAction("Gitee.Accounts.AddGEAccount"), accountList, ActionPlaces.UNKNOWN, null, null)
      }
      appendSecondaryText(" (${KeymapUtil.getFirstKeyboardShortcutText(CommonShortcuts.getNew())})", StatusText.DEFAULT_ATTRIBUTES, null)
    }

    addToCenter(ToolbarDecorator.createDecorator(accountList)
      .disableUpDownActions()
      .setAddAction { showAddAccountActions(it.preferredPopupPoint ?: RelativePoint.getCenterOf(accountList)) }
      .addExtraAction(object : ToolbarDecorator.ElementActionButton(GiteeBundle.message("accounts.set.default"),
        AllIcons.Actions.Checked) {
        override fun actionPerformed(e: AnActionEvent) {
          if (accountList.selectedValue.projectDefault) return
          for (accountData in accountListModel.items) {
            if (accountData == accountList.selectedValue) {
              accountData.projectDefault = true
              accountListModel.contentsChanged(accountData)
            }
            else if (accountData.projectDefault) {
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

  override fun getData(dataId: String): Any? {
    if (KEY.`is`(dataId)) return this
    return null
  }

  private fun showAddAccountActions(point: RelativePoint) {
    val group = actionManager.getAction("Gitee.Accounts.AddAccount") as ActionGroup
    val popup = actionManager.createActionPopupMenu(ActionPlaces.UNKNOWN, group)

    popup.setTargetComponent(this)
    popup.component.show(point.component, point.point.x, point.point.y)
  }

  private fun editAccount(decorator: GiteeAccountDecorator) {
    val dialog = GiteeLoginDialog(executorFactory, project, this).apply {
      withServer(decorator.account.server.toString(), false)
      withCredentials(decorator.account.name)
    }

    if (dialog.showAndGet()) {
      decorator.account.name = dialog.login

      newTokensMap[decorator.account] = dialog.accessToken to dialog.refreshToken

      loadAccountDetails(decorator)
    }
  }

  fun addAccount(server: GiteeServerPath, login: String, tokens: Pair<String, String>) {
    val giteeAccount = GiteeAccountManager.createAccount(login, server)
    newTokensMap[giteeAccount] = tokens

    val accountData = GiteeAccountDecorator(giteeAccount, false)
    accountListModel.add(accountData)
    loadAccountDetails(accountData)
  }

  fun isAccountUnique(login: String, server: GiteeServerPath) =
    accountListModel.items.none { it.account.name == login && it.account.server == server }

  fun loadExistingAccountsDetails() {
    for (accountData in accountListModel.items) {
      loadAccountDetails(accountData)
    }
  }

  private fun loadAccountDetails(accountData: GiteeAccountDecorator) {
    val account = accountData.account
    val tokens = newTokensMap[account] ?: currentTokensMap[account]

    if (tokens == null) {
      accountListModel.contentsChanged(accountData.apply {
        errorText = GiteeBundle.message("account.token.missing")
        showReLoginLink = true
      })
      return
    }

    val executor = executorFactory.create(tokens) {
      newTokens -> GiteeAuthenticationManager.getInstance().updateAccountToken(accountData.account, "${newTokens.first}&${newTokens.second}")
    }

    progressManager.run(object : Task.Backgroundable(project, "Not Visible") {
      lateinit var loadedDetails: GiteeAuthenticatedUser

      override fun run(indicator: ProgressIndicator) {
        loadedDetails = executor.execute(indicator, GiteeApiRequests.CurrentUser.get(account.server))
      }

      override fun onSuccess() {
        accountListModel.contentsChanged(accountData.apply {
          details = loadedDetails
          iconProvider = CachingGiteeAvatarIconsProvider(avatarLoader, imageResizer, executor, GiteeUIUtil.avatarSize, accountList)

          errorText = null
          showReLoginLink = false
        })
      }

      override fun onThrowable(error: Throwable) {
        accountListModel.contentsChanged(accountData.apply {
          errorText = error.message.toString()
          showReLoginLink = error is GiteeAuthenticationException
        })
      }
    })
  }

  private fun createListProgressManager() = object : ProgressVisibilityManager() {
    override fun setProgressVisible(visible: Boolean) = accountList.setPaintBusy(visible)
    override fun getModalityState() = ModalityState.any()
  }

  fun setAccounts(accounts: Map<GiteeAccount, Pair<String, String>?>, defaultAccount: GiteeAccount?) {
    accountListModel.removeAll()
    accountListModel.addAll(0, accounts.keys.map { GiteeAccountDecorator(it, it == defaultAccount) })
    currentTokensMap = accounts
  }

  /**
   * @return list of accounts and associated tokens if new token was created and selected default account
   */
  fun getAccounts(): Pair<Map<GiteeAccount, Pair<String, String>?>, GiteeAccount?> {
    return accountListModel.items.associate { it.account to newTokensMap[it.account] } to
      accountListModel.items.find { it.projectDefault }?.account
  }

  fun clearNewTokens() = newTokensMap.clear()

  fun isModified(accounts: Set<GiteeAccount>, defaultAccount: GiteeAccount?): Boolean {
    return accountListModel.items.find { it.projectDefault }?.account != defaultAccount
      || accountListModel.items.map { it.account }.toSet() != accounts
      || newTokensMap.isNotEmpty()
  }

  override fun dispose() {}

  private inner class GiteeAccountDecoratorRenderer : ListCellRenderer<GiteeAccountDecorator>, JPanel() {
    private val accountName = JLabel()

    private val serverName = JLabel()
    private val profilePicture = JLabel()

    private val fullName = JLabel()

    private val loadingError = JLabel()
    private val reloginLink = LinkLabel<Any?>(GiteeBundle.message("accounts.relogin"), null)

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
        add(reloginLink, bag.next())
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
      UIUtil.setBackgroundRecursively(this, ListUiUtil.WithTallRow.background(list, isSelected, list.hasFocus()))
      val primaryTextColor = ListUiUtil.WithTallRow.foreground(isSelected, list.hasFocus())
      val secondaryTextColor = ListUiUtil.WithTallRow.secondaryForeground(list, isSelected)

      accountName.apply {
        text = value.account.name
        setBold(if (value.details?.name == null) value.projectDefault else false)
        foreground = if (value.details?.name == null) primaryTextColor else secondaryTextColor
      }
      serverName.apply {
        text = value.account.server.toString()
        foreground = secondaryTextColor
      }
      profilePicture.apply {
        icon = value.getIcon()
      }
      fullName.apply {
        text = value.details?.name
        setBold(value.projectDefault)
        isVisible = value.details?.name != null
        foreground = primaryTextColor
      }
      loadingError.apply {
        text = value.errorText
        foreground = UIUtil.getErrorForeground()
      }
      reloginLink.apply {
        isVisible = value.errorText != null && value.showReLoginLink
        setListener(LinkListener { _, _ ->
          editAccount(value)
        }, null)
      }
      return this
    }

    private fun JLabel.setBold(isBold: Boolean) {
      font = font.deriveFont(if (isBold) font.style or Font.BOLD else font.style and Font.BOLD.inv())
    }
  }
}

/**
 * Account + auxillary info + info loading error
 */
private class GiteeAccountDecorator(val account: GiteeAccount, var projectDefault: Boolean) {
  var details: GiteeAuthenticatedUser? = null
  var iconProvider: GiteeAvatarIconsProvider? = null

  var errorText: String? = null
  var showReLoginLink = false

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

  fun getIcon(): Icon? {
    val url = details?.avatarUrl
    return iconProvider?.getIcon(url)
  }
}