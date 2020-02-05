package com.gitee.ui.cloneDialog

import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequestExecutorManager
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GiteeAccountInformationProvider
import com.gitee.icons.GiteeIcons
import com.gitee.util.CachingGiteeUserAvatarLoader
import com.gitee.util.GiteeImageResizer
import com.gitee.util.GiteeUtil
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtension
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionComponent
import com.intellij.openapi.vcs.ui.cloneDialog.VcsCloneDialogExtensionStatusLine
import javax.swing.Icon

class GiteeCloneDialogExtension : VcsCloneDialogExtension {
  private val authenticationManager = GiteeAuthenticationManager.getInstance()

  override fun getName() = GiteeUtil.SERVICE_DISPLAY_NAME

  override fun getIcon(): Icon = GiteeIcons.Gitee_icon

  override fun getAdditionalStatusLines(): List<VcsCloneDialogExtensionStatusLine> {
    if (!authenticationManager.hasAccounts()) {
      return listOf(VcsCloneDialogExtensionStatusLine.greyText("No accounts"))
    }

    val list = ArrayList<VcsCloneDialogExtensionStatusLine>()
    for (account in authenticationManager.getAccounts()) {
      val accName = if (account.server.isGiteeDotCom()) account.name else ("${account.server.host}/${account.name}")
      list.add(VcsCloneDialogExtensionStatusLine.greyText(accName))
    }
    return list
  }

  override fun createMainComponent(project: Project): VcsCloneDialogExtensionComponent {
    throw AssertionError("Shouldn't be called")
  }

  override fun createMainComponent(project: Project, modalityState: ModalityState): VcsCloneDialogExtensionComponent {
    return GiteeCloneDialogExtensionComponent(
        project,
        GiteeAuthenticationManager.getInstance(),
        GiteeApiRequestExecutorManager.getInstance(),
        GiteeApiRequestExecutor.Factory.getInstance(),
        GiteeAccountInformationProvider.getInstance(),
        CachingGiteeUserAvatarLoader.getInstance(),
        GiteeImageResizer.getInstance()
    )
  }
}