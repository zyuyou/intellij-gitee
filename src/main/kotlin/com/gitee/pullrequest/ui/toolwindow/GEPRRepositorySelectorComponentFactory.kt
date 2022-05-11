// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.gitee.pullrequest.ui.toolwindow

import com.gitee.api.GiteeServerPath
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.i18n.GiteeBundle
import com.gitee.ui.component.ComboBoxWithActionsModel
import com.gitee.ui.component.GEAccountSelectorComponentFactory
import com.gitee.ui.component.GERepositorySelectorComponentFactory
import com.gitee.ui.util.getName
import com.gitee.util.GEGitRepositoryMapping
import com.gitee.util.GEProjectRepositoriesManager
import com.gitee.util.GEProjectRepositoriesManager.ListChangeListener
import com.intellij.collaboration.auth.AccountsListener
import com.intellij.collaboration.ui.CollaborationToolsUIUtil.defaultButton
import com.intellij.ide.plugins.newui.HorizontalLayout
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.project.Project
import com.intellij.ui.components.ActionLink
import com.intellij.util.castSafelyTo
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UI
import com.intellij.util.ui.UIUtil
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.layout.PlatformDefaults
import net.miginfocom.swing.MigLayout
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener

class GEPRRepositorySelectorComponentFactory(private val project: Project,
                                             private val authManager: GiteeAuthenticationManager,
                                             private val repositoryManager: GEProjectRepositoriesManager) {
  fun create(disposable: Disposable, onSelected: (GEGitRepositoryMapping, GiteeAccount) -> Unit): JComponent {
    val repositoriesModel = ComboBoxWithActionsModel<GEGitRepositoryMapping>().apply {
      //todo: add remote action
    }
    val accountsModel = ComboBoxWithActionsModel<GiteeAccount>()

    val applyAction = object : AbstractAction(GiteeBundle.message("pull.request.view.list")) {
      override fun actionPerformed(e: ActionEvent?) {
        val repo = repositoriesModel.selectedItem ?: return
        val account = accountsModel.selectedItem ?: return
        onSelected(repo.wrappee, account.wrappee)
      }
    }
    val githubLoginAction = object : AbstractAction(GiteeBundle.message("action.Gitee.Accounts.AddGEAccount.text")) {
      override fun actionPerformed(e: ActionEvent?) {
        authManager.requestNewAccountForDefaultServer(project)?.run {
          applyAction.actionPerformed(e)
        }
      }
    }
    val tokenLoginAction = object : AbstractAction(GiteeBundle.message("action.Gitee.Accounts.AddGEAccountWithToken.text")) {
      override fun actionPerformed(e: ActionEvent?) {
        authManager.requestNewAccountForDefaultServer(project, true)?.run {
          applyAction.actionPerformed(e)
        }
      }
    }
    val gheLoginAction = object : AbstractAction(GiteeBundle.message("action.Gitee.Accounts.AddGEEAccount.text")) {
      override fun actionPerformed(e: ActionEvent?) {
        val server = repositoriesModel.selectedItem?.wrappee?.geRepositoryCoordinates?.serverPath ?: return
        authManager.requestNewAccountForServer(server, project)?.run {
          applyAction.actionPerformed(e)
        }
      }
    }

    Controller(project = project, authManager = authManager, repositoryManager = repositoryManager,
               repositoriesModel = repositoriesModel, accountsModel = accountsModel,
               applyAction = applyAction, githubLoginAction = githubLoginAction, tokenLoginAction = tokenLoginAction,
               gheLoginActon = gheLoginAction,
               disposable = disposable)

    val repoCombo = GERepositorySelectorComponentFactory().create(repositoriesModel).apply {
      putClientProperty(PlatformDefaults.VISUAL_PADDING_PROPERTY, insets)
    }
    val accountCombo = GEAccountSelectorComponentFactory().create(accountsModel).apply {
      putClientProperty(PlatformDefaults.VISUAL_PADDING_PROPERTY, insets)
    }

    val applyButton = JButton(applyAction).defaultButton().apply {
      isOpaque = false
      controlVisibilityFromAction(this, applyAction)
    }

    val githubLoginButton = JButton(githubLoginAction).defaultButton().apply {
      isOpaque = false
      controlVisibilityFromAction(this, githubLoginAction)
    }
    val tokenLoginLink = createLinkLabel(tokenLoginAction)
    val gheLoginButton = JButton(gheLoginAction).defaultButton().apply {
      isOpaque = false
      controlVisibilityFromAction(this, gheLoginAction)
    }
    val actionsPanel = JPanel(HorizontalLayout(UI.scale(16))).apply {
      isOpaque = false
      add(applyButton)
      add(githubLoginButton)
      add(tokenLoginLink)
      add(gheLoginButton)

      putClientProperty(PlatformDefaults.VISUAL_PADDING_PROPERTY, applyButton.insets)
    }

    return JPanel(null).apply {
      isOpaque = false
      border = JBUI.Borders.empty(30, 16)
      layout = MigLayout(LC().fill().gridGap("${UI.scale(10)}px", "${UI.scale(16)}px").insets("0").hideMode(3).noGrid())

      add(repoCombo, CC().growX().push())
      add(accountCombo, CC())

      add(actionsPanel, CC().newline())
      add(JLabel(GiteeBundle.message("pull.request.login.note")).apply {
        foreground = UIUtil.getContextHelpForeground()
      }, CC().newline().minWidth("0"))
    }
  }

  private class Controller(private val project: Project,
                           private val authManager: GiteeAuthenticationManager,
                           private val repositoryManager: GEProjectRepositoriesManager,
                           private val repositoriesModel: ComboBoxWithActionsModel<GEGitRepositoryMapping>,
                           private val accountsModel: ComboBoxWithActionsModel<GiteeAccount>,
                           private val applyAction: Action,
                           private val githubLoginAction: Action,
                           private val tokenLoginAction: Action,
                           private val gheLoginActon: Action,
                           disposable: Disposable) {

    init {
      ApplicationManager.getApplication().messageBus.connect(disposable)
        .subscribe(GEProjectRepositoriesManager.LIST_CHANGES_TOPIC, object : ListChangeListener {
          override fun repositoryListChanged(newList: Set<GEGitRepositoryMapping>, project: Project) {
            updateRepositories()
          }
        })
      authManager.addListener(disposable, object : AccountsListener<GiteeAccount> {
        override fun onAccountListChanged(old: Collection<GiteeAccount>, new: Collection<GiteeAccount>) {
          invokeAndWaitIfNeeded(runnable = ::updateAccounts)
        }
      })

      repositoriesModel.addSelectionChangeListener(::updateAccounts)
      repositoriesModel.addSelectionChangeListener(::updateActions)
      accountsModel.addSelectionChangeListener(::updateActions)

      updateRepositories()
      updateAccounts()
      updateActions()
    }

    private fun updateRepositories() {
      repositoriesModel.items = repositoryManager.knownRepositories.sortedBy { it.gitRemoteUrlCoordinates.remote.name }
      repositoriesModel.preSelect()
    }

    private fun updateAccounts() {
      val serverPath = repositoriesModel.selectedItem?.wrappee?.geRepositoryCoordinates?.serverPath
      if (serverPath == null) {
        accountsModel.items = emptyList()
        accountsModel.actions = emptyList()
        return
      }

      val accounts = authManager.getAccounts()
      val matchingAccounts = accounts.filter { it.server.equals(serverPath, true) }

      accountsModel.items = matchingAccounts
      accountsModel.actions = getAccountsPopupActions(serverPath)
      preselectAccount()
    }

    private fun getAccountsPopupActions(server: GiteeServerPath): List<Action> {
      return if (server.isGiteeDotCom())
        listOf(
          object : AbstractAction(GiteeBundle.message("action.Gitee.Accounts.AddGEAccount.text")) {
            override fun actionPerformed(e: ActionEvent?) {
              authManager.requestNewAccountForDefaultServer(project)?.let(::trySelectAccount)
            }
          },
          object : AbstractAction(GiteeBundle.message("action.Gitee.Accounts.AddGEAccountWithPassword.text")) {
            override fun actionPerformed(e: ActionEvent?) {
              authManager.requestNewAccountForDefaultServer(project, true)?.let(::trySelectAccount)
            }
          }
        )
      else listOf(
        object : AbstractAction(GiteeBundle.message("action.Gitee.Accounts.AddGEEAccount.text")) {
          override fun actionPerformed(e: ActionEvent?) {
            authManager.requestNewAccountForServer(server, project)?.let(::trySelectAccount)
          }
        })
    }

    private fun trySelectAccount(account: GiteeAccount) {
      with(accountsModel) {
        if (size > 0) {
          for (i in 0 until size) {
            val item = getElementAt(i) as? ComboBoxWithActionsModel.Item.Wrapper<GiteeAccount>
            if (item != null && item.wrappee.castSafelyTo<GiteeAccount>() == account) {
              selectedItem = item
              break
            }
          }
        }
      }
    }

    private fun preselectAccount() {
      with(accountsModel) {
        if (selectedItem == null && size > 0) {
          val defaultAccount = authManager.getDefaultAccount(project)
          var newSelection = getElementAt(0) as? ComboBoxWithActionsModel.Item.Wrapper
          for (i in 0 until size) {
            val item = getElementAt(i) as? ComboBoxWithActionsModel.Item.Wrapper
            if (item != null && item.wrappee.castSafelyTo<GiteeAccount>() == defaultAccount) {
              newSelection = item
              break
            }
          }
          selectedItem = newSelection
        }
      }
    }

    private fun updateActions() {
      val hasAccounts = accountsModel.items.isNotEmpty()
      val serverPath = repositoriesModel.selectedItem?.wrappee?.geRepositoryCoordinates?.serverPath
      val isGiteeServer = serverPath?.isGiteeDotCom() ?: false

      applyAction.isEnabled = accountsModel.selectedItem != null
      applyAction.visible = hasAccounts
      githubLoginAction.visible = !hasAccounts && isGiteeServer
      tokenLoginAction.visible = !hasAccounts && isGiteeServer
      gheLoginActon.visible = !hasAccounts && !isGiteeServer
    }
  }

  companion object {
    private const val ACTION_VISIBLE_KEY = "ACTION_VISIBLE"

    private fun controlVisibilityFromAction(button: JButton, action: Action) {
      fun update() {
        button.isVisible = action.getValue(ACTION_VISIBLE_KEY) as? Boolean ?: true
      }
      action.addPropertyChangeListener {
        update()
      }
      update()
    }

    private var Action.visible: Boolean
      get() = getValue(ACTION_VISIBLE_KEY) as? Boolean ?: true
      set(value) = putValue(ACTION_VISIBLE_KEY, value)

    fun createLinkLabel(action: Action): ActionLink {
      val label = ActionLink(action.getName()) {
        action.actionPerformed(it)
      }
      label.isEnabled = action.isEnabled
      label.isVisible = action.getValue(ACTION_VISIBLE_KEY) as? Boolean ?: true

      action.addPropertyChangeListener {
        label.text = action.getName()
        label.isEnabled = action.isEnabled
        label.isVisible = action.getValue(ACTION_VISIBLE_KEY) as? Boolean ?: true
      }
      return label
    }

    private fun <T> ComboBoxModel<T>.addSelectionChangeListener(listener: () -> Unit) {
      addListDataListener(object : ListDataListener {
        override fun contentsChanged(e: ListDataEvent) {
          @Suppress("UNCHECKED_CAST")
          if (e.index0 == -1 && e.index1 == -1) listener()
        }

        override fun intervalAdded(e: ListDataEvent) {}
        override fun intervalRemoved(e: ListDataEvent) {}
      })
    }

    private fun ComboBoxWithActionsModel<GEGitRepositoryMapping>.preSelect() {
      if (selectedItem != null) return
      if (size == 0) return
      selectedItem = getElementAt(0) as? ComboBoxWithActionsModel.Item.Wrapper
    }
  }
}