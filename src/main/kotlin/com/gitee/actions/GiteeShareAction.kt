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

package com.gitee.actions

import com.gitee.api.GiteeApiRequestExecutorManager
import com.gitee.api.GiteeApiRequests
import com.gitee.api.data.request.GiteeRequestPagination
import com.gitee.api.data.request.Type
import com.gitee.api.util.GiteeApiPagesLoader
import com.gitee.authentication.GiteeAuthenticationManager
import com.gitee.authentication.accounts.GiteeAccount
import com.gitee.authentication.accounts.GiteeAccountInformationProvider
import com.gitee.i18n.GiteeBundle
import com.gitee.icons.GiteeIcons
import com.gitee.ui.GiteeShareDialog
import com.gitee.util.*
import com.intellij.CommonBundle
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.components.service
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.Splitter
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.vcs.*
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.changes.ui.SelectFilesDialog
import com.intellij.openapi.vcs.ui.CommitMessage
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.components.BrowserLink
import com.intellij.ui.components.JBLabel
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.mapSmartSet
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.intellij.vcsUtil.VcsFileUtil
import git4idea.DialogManager
import git4idea.GitUtil
import git4idea.actions.GitInit
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import git4idea.i18n.GitBundle
import git4idea.repo.GitRepository
import git4idea.util.GitFileUtils
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.TestOnly
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Container
import java.io.IOException
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/GithubShareAction.kt
 * @author JetBrains s.r.o.
 */
class GiteeShareAction : DumbAwareAction(GiteeBundle.message2("gitee.share.project.title"), GiteeBundle.message2("gitee.share.project.desc"), GiteeIcons.Gitee_icon) {

  override fun update(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT)
    e.presentation.isEnabledAndVisible = project != null && !project.isDefault
  }

  // get gitRepository
  // check for existing git repo
  // check available repos and privateRepo access (net)
  // Show dialog (window)
  // create Gitee repo (net)
  // create local git repo (if not exist)
  // add Gitee as a remote host
  // make first commit
  // push everything (net)
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.getData(CommonDataKeys.PROJECT)
    val file = e.getData(CommonDataKeys.VIRTUAL_FILE)

    if (project == null || project.isDisposed) {
      return
    }

    shareProjectOnGitee(project, file)
  }

  companion object {
    private val LOG = GiteeUtil.LOG

    @JvmStatic
    fun shareProjectOnGitee(project: Project, file: VirtualFile?) {
      FileDocumentManager.getInstance().saveAllDocuments();

      val gitRepository = GiteeGitHelper.findGitRepository(project, file)

      val possibleRemotes = gitRepository
        ?.let(project.service<GEProjectRepositoriesManager>()::findKnownRepositories)
        ?.map { it.gitRemoteUrlCoordinates.url }.orEmpty()

      if (possibleRemotes.isNotEmpty()) {
        val existingRemotesDialog = GiteeExistingRemotesDialog(project, possibleRemotes)
        DialogManager.show(existingRemotesDialog)
        if (!existingRemotesDialog.isOK) {
          return
        }
      }

      val authManager = service<GiteeAuthenticationManager>()
      val progressManager = service<ProgressManager>()
      val requestExecutorManager = service<GiteeApiRequestExecutorManager>()
      val accountInformationProvider = service<GiteeAccountInformationProvider>()
      val gitHelper = service<GiteeGitHelper>()
      val git = service<Git>()

//      if (!authManager.ensureHasAccounts(project)) return
//      val accounts = authManager.getAccounts()
//      val progressManager = service<ProgressManager>()
//      val requestExecutorManager = service<GiteeApiRequestExecutorManager>()
//      val accountInformationProvider = service<GiteeAccountInformationProvider>()
//      val gitHelper = service<GiteeGitHelper>()
//      val git = service<Git>()
//      val possibleRemotes = gitRepository?.let(gitHelper::getAccessibleRemoteUrls).orEmpty()

//      if (possibleRemotes.isNotEmpty()) {
//        val existingRemotesDialog = GiteeExistingRemotesDialog(project, possibleRemotes)
//        DialogManager.show(existingRemotesDialog)
//        if (!existingRemotesDialog.isOK) {
//          return
//        }
//      }

      val accountInformationLoader = object : (GiteeAccount, Component) -> Pair<Boolean, Set<String>> {
        private val loadedInfo = mutableMapOf<GiteeAccount, Pair<Boolean, Set<String>>>()

        @Throws(IOException::class)
        override fun invoke(account: GiteeAccount, parentComponent: Component) = loadedInfo.getOrPut(account) {
          val requestExecutor = requestExecutorManager.getExecutor(account, parentComponent) ?: throw ProcessCanceledException()

          progressManager.runProcessWithProgressSynchronously(ThrowableComputable<Pair<Boolean, Set<String>>, IOException> {

            val user = requestExecutor.execute(progressManager.progressIndicator, GiteeApiRequests.CurrentUser.get(account.server))

            val names = GiteeApiPagesLoader.loadAll(
              requestExecutor,
              progressManager.progressIndicator,
              GiteeApiRequests.CurrentUser.Repos.pages(account.server, Type.OWNER, pagination = GiteeRequestPagination.DEFAULT)
            ).mapSmartSet { it.name }

            user.canCreatePrivateRepo() to names
          }, GiteeBundle.message("share.process.loading.account.info", account), true, project)
        }
      }

      val shareDialog = GiteeShareDialog(project, authManager.getAccounts(), authManager.getDefaultAccount(project),
        gitRepository?.remotes?.map { it.name }?.toSet() ?: emptySet(), accountInformationLoader)

      DialogManager.show(shareDialog)
      if (!shareDialog.isOK) {
        return
      }

      val name: String = shareDialog.getRepositoryName()
      val isPrivate: Boolean = shareDialog.isPrivate()
      val remoteName: String = shareDialog.getRemoteName()
      val description: String = shareDialog.getDescription()
      val account: GiteeAccount = shareDialog.getAccount()!!

      val requestExecutor = requestExecutorManager.getExecutor(account, project) ?: return

      object : Task.Backgroundable(project, GiteeBundle.message("share.process")) {
        private lateinit var url: String

        override fun run(indicator: ProgressIndicator) {
          // create Gitee repo (network)
          LOG.info("Creating Gitee repository")

          indicator.text = GiteeBundle.message("share.process.creating.repository")

          url = requestExecutor.execute(indicator, GiteeApiRequests.CurrentUser.Repos.create(account.server, name, description, isPrivate)).htmlUrl
          LOG.info("Successfully created Gitee repository")

          val root = gitRepository?.root ?: project.baseDir

          // creating empty git repo if git is not initialized
          LOG.info("Binding local project with Gitee")

          if (gitRepository == null) {
            LOG.info("No git detected, creating empty git repo")
            indicator.text = GiteeBundle.message("share.process.creating.git.repository")
            if (!createEmptyGitRepository(project, root)) {
              return
            }
          }

          val repositoryManager = GitUtil.getRepositoryManager(project)
          val repository = repositoryManager.getRepositoryForRoot(root)

          if (repository == null) {
            GiteeNotifications.showError(project,
              GiteeNotificationIdsHolder.SHARE_CANNOT_FIND_GIT_REPO,
              GiteeBundle.message("share.error.failed.to.create.repo"),
              GiteeBundle.message("cannot.find.git.repo"))
            return
          }

          indicator.text = GiteeBundle.message("share.process.retrieving.username")
          val username = accountInformationProvider.getInformation(requestExecutor, indicator, account).login
          val remoteUrl = gitHelper.getRemoteUrl(account.server, username, name)

          //git remote add origin git@gitee.com:login/name.git
          LOG.info("Adding Gitee as a remote host")
          indicator.text = GiteeBundle.message("share.process.adding.gh.as.remote.host")
          git.addRemote(repository, remoteName, remoteUrl).getOutputOrThrow()
          repository.update()

          // create sample commit for binding project
          if (!performFirstCommitIfRequired(project, root, repository, indicator, name, url)) {
            return
          }

          //git push origin master
          LOG.info("Pushing to gitee master")
          indicator.text = GiteeBundle.message("share.process.pushing.to.gitee.master")
          if (!pushCurrentBranch(project, repository, remoteName, remoteUrl, name, url)) {
            return
          }

          GiteeNotifications.showInfoURL(project, GiteeNotificationIdsHolder.SHARE_PROJECT_SUCCESSFULLY_SHARED,
            GiteeBundle.message("share.process.successfully.shared"), name, url)
        }

        private fun createEmptyGitRepository(project: Project, root: VirtualFile): Boolean {
          val result = Git.getInstance().init(project, root)
          if (!result.success()) {
            VcsNotifier.getInstance(project).notifyError(GiteeNotificationIdsHolder.GIT_REPO_INIT_REPO,
              GitBundle.message("initializing.title"), result.errorOutputAsHtmlString)

            LOG.info("Failed to create empty git repo: " + result.errorOutputAsJoinedString)
            return false
          }

          GitInit.refreshAndConfigureVcsMappings(project, root, root.path)
          GitUtil.generateGitignoreFileIfNeeded(project, root)
          return true
        }

        private fun performFirstCommitIfRequired(project: Project,
                                                 root: VirtualFile,
                                                 repository: GitRepository,
                                                 indicator: ProgressIndicator,
                                                 @NlsSafe name: String,
                                                 url: String): Boolean {
          // check if there is no commits
          if (!repository.isFresh) {
            return true
          }

          LOG.info("Trying to commit")
          try {
            LOG.info("Adding files for commit")
            indicator.text = GiteeBundle.message("share.process.adding.files")

            // ask for files to add
            val trackedFiles = ChangeListManager.getInstance(project).affectedFiles
            val untrackedFiles =
              filterOutIgnored(project, repository.untrackedFilesHolder.retrieveUntrackedFilePaths().mapNotNull(FilePath::getVirtualFile))
            trackedFiles.removeAll(untrackedFiles) // fix IDEA-119855

            val allFiles = ArrayList<VirtualFile>()
            allFiles.addAll(trackedFiles)
            allFiles.addAll(untrackedFiles)

            val dialog = invokeAndWaitIfNeeded(indicator.modalityState) {
              GiteeUntrackedFilesDialog(project, allFiles).apply {
                if (!trackedFiles.isEmpty()) {
                  selectedFiles = trackedFiles
                }
                DialogManager.show(this)
              }
            }

            val files2commit = dialog.selectedFiles
            if (!dialog.isOK || files2commit.isEmpty()) {
              GiteeNotifications.showInfoURL(project, GiteeNotificationIdsHolder.SHARE_EMPTY_REPO_CREATED,
                GiteeBundle.message("share.process.empty.project.created"), name, url)
              return false
            }

            val files2add = ContainerUtil.intersection(untrackedFiles, files2commit)
            val files2rm = ContainerUtil.subtract(trackedFiles, files2commit)
            val modified = HashSet(trackedFiles)
            modified.addAll(files2commit)

            GitFileUtils.addFiles(project, root, files2add)
            GitFileUtils.deleteFilesFromCache(project, root, files2rm)

            // commit
            LOG.info("Performing commit")
            indicator.text = GiteeBundle.message("share.process.performing.commit")

            val handler = GitLineHandler(project, root, GitCommand.COMMIT)
            handler.setStdoutSuppressed(false)
            handler.addParameters("-m", dialog.commitMessage)
            handler.endOptions()
            Git.getInstance().runCommand(handler).getOutputOrThrow()

            VcsFileUtil.markFilesDirty(project, modified)
          } catch (e: VcsException) {
            LOG.warn(e)

            GiteeNotifications.showErrorURL(project, GiteeNotificationIdsHolder.SHARE_PROJECT_INIT_COMMIT_FAILED,
              GiteeBundle.message("share.error.cannot.finish"),
              GiteeBundle.message("share.error.created.project"),
              " '$name' ",
              GiteeBundle.message("share.error.init.commit.failed") + GiteeUtil.getErrorTextFromException(e),
              url)
            return false
          }

          LOG.info("Successfully created initial commit")
          return true
        }

        private fun filterOutIgnored(project: Project, files: Collection<VirtualFile>): Collection<VirtualFile> {
          val changeListManager = ChangeListManager.getInstance(project)
          val vcsManager = ProjectLevelVcsManager.getInstance(project)
          return ContainerUtil.filter(files) { file -> !changeListManager.isIgnoredFile(file) && !vcsManager.isIgnored(file) }
        }

        private fun pushCurrentBranch(project: Project,
                                      repository: GitRepository,
                                      remoteName: String,
                                      remoteUrl: String,
                                      name: String,
                                      url: String): Boolean {
          val currentBranch = repository.currentBranch
          if (currentBranch == null) {
            GiteeNotifications.showErrorURL(project, GiteeNotificationIdsHolder.SHARE_PROJECT_INIT_PUSH_FAILED,
              GiteeBundle.message("share.error.cannot.finish"),
              GiteeBundle.message("share.error.created.project"),
              " '$name' ",
              GiteeBundle.message("share.error.push.no.current.branch"),
              url)
            return false
          }

          val result = git.push(repository, remoteName, remoteUrl, currentBranch.name, true)
          if (!result.success()) {
            GiteeNotifications.showErrorURL(project, GiteeNotificationIdsHolder.SHARE_PROJECT_INIT_PUSH_FAILED,
              GiteeBundle.message("share.error.cannot.finish"),
              GiteeBundle.message("share.error.created.project"),
              " '$name' ",
              GiteeBundle.message("share.error.push.failed", result.errorOutputAsHtmlString),
              url)
            return false
          }

          return true
        }

        override fun onThrowable(error: Throwable) {
          GiteeNotifications.showError(project,
            GiteeNotificationIdsHolder.SHARE_CANNOT_CREATE_REPO,
            GiteeBundle.message("share.error.failed.to.create.repo"), error)
        }
      }.queue()
    }
  }

  @TestOnly
  class GiteeExistingRemotesDialog(project: Project, private val remotes: List<String>) : DialogWrapper(project) {
    init {
      title = GiteeBundle.message2("gitee.project.is.already.on.text")
      setOKButtonText("Share Anyway")
      init()
    }

    override fun createCenterPanel(): JComponent? {
      val mainText = JBLabel(
        if (remotes.size == 1) GiteeBundle.message("share.action.remote.is.on.gitee") else GiteeBundle.message("share.action.remotes.are.on.gitee")
      )

      val remotesPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
      }
      for (remote in remotes) {
        remotesPanel.add(BrowserLink(remote, remote))
      }

      val messagesPanel = JBUI.Panels.simplePanel(UIUtil.DEFAULT_HGAP, UIUtil.DEFAULT_VGAP)
        .addToTop(mainText)
        .addToCenter(remotesPanel)

      val iconContainer = Container().apply {
        layout = BorderLayout()
        add(JLabel(Messages.getQuestionIcon()), BorderLayout.NORTH)
      }
      return JBUI.Panels.simplePanel(UIUtil.DEFAULT_HGAP, UIUtil.DEFAULT_VGAP)
        .addToCenter(messagesPanel)
        .addToLeft(iconContainer)
        .apply { border = JBUI.Borders.emptyBottom(UIUtil.LARGE_VGAP) }
    }
  }

  @TestOnly
  class GiteeUntrackedFilesDialog(private val myProject: Project, untrackedFiles: List<VirtualFile>) :
    SelectFilesDialog(myProject, untrackedFiles, null, null, true, false),
    DataProvider {
    private var myCommitMessagePanel: CommitMessage? = null

    val commitMessage: String
      get() = myCommitMessagePanel!!.comment

    init {
      title = "Add Files For Initial Commit"
      setOKButtonText(CommonBundle.getAddButtonText())
      setCancelButtonText(CommonBundle.getCancelButtonText())
      init()
    }

    override fun createNorthPanel(): JComponent? {
      return null
    }

    override fun createCenterPanel(): JComponent? {
      val tree = super.createCenterPanel()

      myCommitMessagePanel = CommitMessage(myProject)
      myCommitMessagePanel!!.setCommitMessage("Initial commit")

      val splitter = Splitter(true)
      splitter.setHonorComponentsMinimumSize(true)
      splitter.firstComponent = tree
      splitter.secondComponent = myCommitMessagePanel
      splitter.proportion = 0.7f

      return splitter
    }

    override fun getData(@NonNls dataId: String): Any? {
      return if (VcsDataKeys.COMMIT_MESSAGE_CONTROL.`is`(dataId)) {
        myCommitMessagePanel
      } else null
    }

    override fun getDimensionServiceKey(): String? {
      return "Gitee.UntrackedFilesDialog"
    }
  }
}