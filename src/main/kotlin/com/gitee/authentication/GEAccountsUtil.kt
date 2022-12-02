package com.gitee.authentication

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.gitee.api.GiteeServerPath
import com.gitee.authentication.accounts.GEAccountManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.accounts.GiteeProjectDefaultAccountHolder
import com.gitee.authentication.ui.GELoginDialog
import com.gitee.authentication.ui.GELoginModel
import com.gitee.i18n.GiteeBundle
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.components.service
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.NlsContexts
import com.intellij.util.AuthData
import com.intellij.util.concurrency.annotations.RequiresEdt
import git4idea.DialogManager
import git4idea.i18n.GitBundle
import io.ktor.util.reflect.*
import java.awt.Component
import javax.swing.JComponent

private val accountManager: GEAccountManager get() = service()

object GEAccountsUtil {
  data class GEAppCredentials(val clientId: String, val clientSecret: String)

  val jacksonMapper: ObjectMapper get() = jacksonObjectMapper()

  const val APP_CLIENT_ID: String = "b7837ec65bcb294b0e2a31e5669b788a3185829524af4d818c3c2a35b186366d"
  const val APP_CLIENT_SECRET: String = "85891acdb745502e19e02e3bbcd405dd303190c8a3fcf29a6ca7a2796b76f918"
  const val APP_CLIENT_SCOPE: String = "user_info projects pull_requests gists issues notes groups"

  /**
   * 获取默认Gitee应用凭证
   * */
  fun getDefaultGEAppCredentials(): GEAppCredentials {
    return GEAppCredentials(APP_CLIENT_ID, APP_CLIENT_SECRET)
  }

  @JvmStatic
  val accounts: Set<GiteeAccount>
    get() = accountManager.accountsState.value

  @JvmStatic
  fun getDefaultAccount(project: Project): GiteeAccount? =
    project.service<GiteeProjectDefaultAccountHolder>().account

  @JvmStatic
  fun setDefaultAccount(project: Project, account: GiteeAccount?) {
    project.service<GiteeProjectDefaultAccountHolder>().account = account
  }

  @JvmStatic
  fun getSingleOrDefaultAccount(project: Project): GiteeAccount? =
    getDefaultAccount(project) ?: accounts.singleOrNull()

  internal fun createAddAccountActionGroup(model: GELoginModel, project: Project, parentComponent: JComponent): ActionGroup {
    val group = DefaultActionGroup()

    group.add(
      DumbAwareAction.create(GiteeBundle.message("action.Gitee.Accounts.AddGEAccount.text")) {
        GELoginDialog.OAuth(model, project, parentComponent).apply {
          setServer(GiteeServerPath.DEFAULT_HOST, false)
          showAndGet()
        }
      })

    group.add(
      DumbAwareAction.create(GiteeBundle.message("action.Gitee.Accounts.AddGEAccountWithPassword.text")) {
        GELoginDialog.Password(model, project, parentComponent).apply {
          title = GiteeBundle.message("dialog.title.add.gitee.account")
          setLoginButtonText(GitBundle.message("login.dialog.button.login"))
          setServer(GiteeServerPath.DEFAULT_HOST, false)
          showAndGet()
        }
      }
    )

    group.add(
      DumbAwareAction.create(GiteeBundle.message("action.Gitee.Accounts.AddGEAccountWithTokens.text")) {
        GELoginDialog.Tokens(model, project, parentComponent).apply {
          title = GiteeBundle.message("dialog.title.add.gitee.account")
          setLoginButtonText(GiteeBundle.message("accounts.add.button"))
          setServer(GiteeServerPath.DEFAULT_HOST, false)
          showAndGet()
        }
      }
    )

    group.add(Separator())

    group.add(
      DumbAwareAction.create(GiteeBundle.message("action.Gitee.Accounts.AddGEEAccount.text")) {
        GELoginDialog.Tokens(model, project, parentComponent).apply {
          title = GiteeBundle.message("dialog.title.add.gitee.account")
          setServer("", true)
          setLoginButtonText(GiteeBundle.message("accounts.add.button"))
          showAndGet()
        }
      }
    )
    return group
  }

  @RequiresEdt
  @JvmOverloads
  @JvmStatic
  fun requestNewAccount(
    server: GiteeServerPath? = null,
    login: String? = null,
    project: Project?,
    parentComponent: Component? = null,
    authType: AuthorizationType = AuthorizationType.UNDEFINED
  ): GEAccountAuthData? {
    val model = AccountManagerLoginModel()
    login(
      model, GELoginRequest(server = server, login = login, isLoginEditable = login != null, authType = authType),
      project, parentComponent
    )
    return model.authData
  }

  @RequiresEdt
  @JvmOverloads
  @JvmStatic
  internal fun requestNewCredentials(
    account: GiteeAccount,
    project: Project?,
    parentComponent: Component? = null
  ): GECredentials? {
    val model = AccountManagerLoginModel(account)
    login(
      model,
      GELoginRequest(
        text = GiteeBundle.message("account.credentials.missing.for", account),
        server = account.server, login = account.name
      ),
      project, parentComponent,
    )
    return model.authData?.credentials
  }

  @RequiresEdt
  @JvmOverloads
  @JvmStatic
  fun requestReLogin(
    account: GiteeAccount,
    project: Project?,
    parentComponent: Component? = null,
    authType: AuthorizationType = AuthorizationType.UNDEFINED
  ): GEAccountAuthData? {
    val model = AccountManagerLoginModel(account)
    login(
      model, GELoginRequest(server = account.server, login = account.name, authType = authType),
      project, parentComponent)
    return model.authData
  }

  @RequiresEdt
  @JvmStatic
  internal fun login(model: GELoginModel, request: GELoginRequest, project: Project?, parentComponent: Component?) {
    if (request.server != GiteeServerPath.DEFAULT_SERVER) {
      if(request.credentials != null) {
        request.loginRefreshTokens(model, project, parentComponent)
      } else {
        request.loginWithOAuthOrTokens(model, project, parentComponent)
      }
    }
    else when (request.authType) {
      AuthorizationType.OAUTH -> request.loginWithOAuth(model, project, parentComponent)
      AuthorizationType.TOKENS -> request.loginWithTokens(model, project, parentComponent)
      AuthorizationType.UNDEFINED ->
        if(request.credentials != null) {
          request.loginRefreshTokens(model, project, parentComponent)
        } else {
          request.loginWithOAuthOrTokens(model, project, parentComponent)
        }
    }
  }
}

class GEAccountAuthData(val account: GiteeAccount, login: String, val credentials: GECredentials) : AuthData(login, credentials.accessToken) {
  val server: GiteeServerPath get() = account.server
//  val token: String get() = password!!
}

internal class GELoginRequest(
  val text: @NlsContexts.DialogMessage String? = null,
  val error: Throwable? = null,

  val server: GiteeServerPath? = null,
  val isServerEditable: Boolean = server == null,

  val login: String? = null,
  val isLoginEditable: Boolean = true,

  val credentials: GECredentials? = null,
  val authType: AuthorizationType = AuthorizationType.UNDEFINED
)

private fun GELoginRequest.configure(dialog: GELoginDialog) {
  error?.let { dialog.setError(it) }
  server?.let { dialog.setServer(it.toString(), isServerEditable) }
  login?.let { dialog.setLogin(it, isLoginEditable) }
}

private fun GELoginRequest.loginWithOAuth(model: GELoginModel, project: Project?, parentComponent: Component?) {
  val dialog = GELoginDialog.OAuth(model, project, parentComponent)
  configure(dialog)
  DialogManager.show(dialog)
}

private fun GELoginRequest.loginWithTokens(model: GELoginModel, project: Project?, parentComponent: Component?) {
  val dialog = GELoginDialog.Tokens(model, project, parentComponent)
  configure(dialog)
  DialogManager.show(dialog)
}

private fun GELoginRequest.loginWithPassword(model: GELoginModel, project: Project?, parentComponent: Component?) {
  val dialog = GELoginDialog.Password(model, project, parentComponent)
  configure(dialog)
  DialogManager.show(dialog)
}

private fun GELoginRequest.loginRefreshTokens(model: GELoginModel, project: Project?, parentComponent: Component?) {
  val dialog = GELoginDialog.RefreshToken(model, project, parentComponent)
  configure(dialog)
  DialogManager.show(dialog)
}

private fun GELoginRequest.loginWithOAuthOrTokens(model: GELoginModel, project: Project?, parentComponent: Component?) {
  when (promptOAuthLogin(this, project, parentComponent)) {
    Messages.YES ->
      loginWithOAuth(model, project, parentComponent)
    Messages.NO ->
      when(promptTokenLogin(this, project, parentComponent)) {
        Messages.YES ->
          loginWithPassword(model, project, parentComponent)
        Messages.NO ->
          loginWithTokens(model, project, parentComponent)
      }
  }
}

private fun promptOAuthLogin(request: GELoginRequest, project: Project?, parentComponent: Component?): Int {
  val builder = MessageDialogBuilder.yesNoCancel(title = GiteeBundle.message("login.to.gitee"),
    message = request.text ?: GiteeBundle.message("dialog.message.login.to.continue"))
    .yesText(GiteeBundle.message("login.via.gitee.action"))
    .noText(GiteeBundle.message("button.use.other"))
    .icon(Messages.getWarningIcon())
  if (parentComponent != null) {
    return builder.show(parentComponent)
  }
  else {
    return builder.show(project)
  }
}

private fun promptTokenLogin(request: GELoginRequest, project: Project?, parentComponent: Component?): Int {
  val builder = MessageDialogBuilder.yesNoCancel(GiteeBundle.message("login.to.gitee"),
    message = request.text?: GiteeBundle.message("dialog.message.login.to.continue"))
    .yesText(GiteeBundle.message("button.use.password"))
    .noText(GiteeBundle.message("button.use.tokens"))
    .icon(Messages.getWarningIcon())

  if (parentComponent != null) {
    return builder.show(parentComponent)
  } else {
    return builder.show(project)
  }
}

private class AccountManagerLoginModel(private val account: GiteeAccount? = null) : GELoginModel {
  private val accountManager: GEAccountManager = service()

  var authData: GEAccountAuthData? = null

  override fun isAccountUnique(server: GiteeServerPath, login: String): Boolean =
    accountManager.accountsState.value.filter {
      it != account
    }.none {
      it.name == login && it.server.equals(server, true)
    }

  override suspend fun saveLogin(server: GiteeServerPath, login: String, credentials: GECredentials) {
    val acc = account ?: GEAccountManager.createAccount(login, server)
    acc.name = login
    accountManager.updateAccount(acc, credentials)
    authData = GEAccountAuthData(acc, login, credentials)
  }
}