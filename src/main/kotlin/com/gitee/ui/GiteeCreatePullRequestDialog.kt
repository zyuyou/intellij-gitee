/*
 * Copyright 2016-2018 码云 - Gitee
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gitee.ui

import com.gitee.util.GiteeCreatePullRequestWorker
import com.gitee.util.GiteeCreatePullRequestWorker.BranchInfo
import com.gitee.util.GiteeCreatePullRequestWorker.ForkInfo
import com.gitee.util.GiteeNotifications
import com.gitee.util.GiteeProjectSettings
import com.gitee.util.GiteeSettings
import com.intellij.CommonBundle
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.ThreeState
import java.awt.event.ItemEvent
import javax.swing.JComponent

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/ui/GithubCreatePullRequestDialog.java
 * @author JetBrains s.r.o.
 */
class GiteeCreatePullRequestDialog(val project: Project,
                                   private val worker: GiteeCreatePullRequestWorker) : DialogWrapper(project) {

  private val projectSettings: GiteeProjectSettings = GiteeProjectSettings.getInstance(project)
  private val centerPanel: GiteeCreatePullRequestPanel = GiteeCreatePullRequestPanel()

  companion object {
    private val ourDoNotAskOption: CreateRemoteDoNotAskOption = CreateRemoteDoNotAskOption()
  }

  init {
    centerPanel.showDiffButton.addActionListener { _ ->
      worker.showDiffDialog(centerPanel.getSelectedBranch())
    }

    centerPanel.selectForkButton.addActionListener { _ ->
      val fork = worker.showTargetDialog()
      if (fork != null) {
        centerPanel.setForks(worker.forks)
        centerPanel.setSelectedFork(fork.path)
      }
    }

    centerPanel.forkComboBox.addItemListener { e ->
      if (e.stateChange == ItemEvent.DESELECTED) {
        centerPanel.setBranches(emptyList())
      }

      if (e.stateChange == ItemEvent.SELECTED) {
        val fork = e.item as ForkInfo? ?: return@addItemListener

        centerPanel.setBranches(fork.branches)
        centerPanel.setSelectedBranch(fork.defaultBranch)

        if (fork.remoteName == null && !fork.isProposedToCreateRemote) {
          fork.isProposedToCreateRemote = true

          val createRemote = when (GiteeSettings.getInstance().createPullRequestCreateRemote) {
            ThreeState.YES -> true
            ThreeState.NO -> false
            ThreeState.UNSURE ->
              GiteeNotifications.showYesNoDialog(
                project,
                "Can't Find Remote",
                "Configure remote for '" + fork.path.user + "'?",
                ourDoNotAskOption
              )
          }

          if (createRemote) {
            worker.configureRemote(fork)
          }
        }

        if (fork.remoteName == null) {
          centerPanel.setDiffEnabled(false)
        } else {
          centerPanel.setDiffEnabled(true)
          worker.launchFetchRemote(fork)
        }
      }
    }

    centerPanel.branchComboBox.addItemListener { e ->
      if (e.stateChange == ItemEvent.SELECTED) {
        val branch = e.item as BranchInfo? ?: return@addItemListener

        if (branch.forkInfo.remoteName != null) {
          if (branch.diffInfoTask != null && branch.diffInfoTask!!.isDone && branch.diffInfoTask!!.safeGet() == null) {
            centerPanel.setDiffEnabled(false)
          } else {
            centerPanel.setDiffEnabled(true)
          }
        }

        if (centerPanel.isTitleDescriptionEmptyOrNotModified()) {
          val description = worker.getDefaultDescriptionMessage(branch)
          centerPanel.setTitle(description.getFirst())
          centerPanel.setDescription(description.getSecond())
        }

        worker.launchLoadDiffInfo(branch)
      }
    }

    centerPanel.setForks(worker.forks)

    val defaultRepo = projectSettings.createPullRequestDefaultRepo
    val defaultBranch = projectSettings.createPullRequestDefaultBranch

    centerPanel.setSelectedFork(defaultRepo)
    if (defaultBranch != null) { // do not rewrite default value of Fork.getDefaultBranch() by null
      centerPanel.setSelectedBranch(defaultBranch)
    }

    title = "Create Pull Request - " + worker.currentBranch

    init()
  }

  override fun getHelpId(): String? {
    return "gitee.create.pull.request.dialog"
  }

  override fun getDimensionServiceKey(): String? {
    return "Gitee.CreatePullRequestDialog"
  }

  override fun createCenterPanel(): JComponent? {
    return centerPanel.panel
  }

  override fun getPreferredFocusedComponent(): JComponent? {
    return centerPanel.getPreferredComponent()
  }

  override fun doOKAction() {
    val branch = centerPanel.getSelectedBranch()

    if (worker.checkAction(branch)) {
      assert(branch != null)

      worker.createPullRequest(branch!!, getRequestTitle(), getDescription())

      projectSettings.setCreatePullRequestDefaultBranch(branch.remoteName)
      projectSettings.setCreatePullRequestDefaultRepo(branch.forkInfo.path)

      super.doOKAction()
    }
  }

  override fun doValidate(): ValidationInfo? {
    return if (StringUtil.isEmptyOrSpaces(getRequestTitle())) {
      ValidationInfo("Title can't be empty'", centerPanel.getTitleTextField())
    } else {
      null
    }
  }

  private fun getRequestTitle(): String {
    return centerPanel.getTitle()
  }

  private fun getDescription(): String {
    return centerPanel.getDescription()
  }

  private class CreateRemoteDoNotAskOption : DialogWrapper.DoNotAskOption {
    override fun isToBeShown(): Boolean {
      return true
    }

    override fun setToBeShown(value: Boolean, exitCode: Int) {
      when {
        value ->
          GiteeSettings.getInstance().createPullRequestCreateRemote = ThreeState.UNSURE
        exitCode == DialogWrapper.OK_EXIT_CODE ->
          GiteeSettings.getInstance().createPullRequestCreateRemote = ThreeState.YES
        else ->
          GiteeSettings.getInstance().createPullRequestCreateRemote = ThreeState.NO
      }
    }

    override fun canBeHidden(): Boolean {
      return true
    }

    override fun shouldSaveOptionsOnCancel(): Boolean {
      return false
    }

    override fun getDoNotShowMessage(): String {
      return CommonBundle.message("dialog.options.do.not.ask")
    }
  }


}