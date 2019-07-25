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

package com.gitee.util

import com.gitee.GiteeBundle
import com.gitee.api.GiteeApiRequestExecutor
import com.gitee.api.GiteeApiRequests
import com.gitee.api.GiteeFullPath
import com.gitee.api.GiteeServerPath
import com.gitee.api.data.GiteePullRequest
import com.gitee.api.data.GiteeRepo
import com.gitee.api.util.GiteeApiPagesLoader
import com.gitee.exceptions.GiteeConfusingException
import com.gitee.exceptions.GiteeOperationCanceledException
import com.gitee.ui.GiteeSelectForkDialog
import com.intellij.dvcs.ui.CompareBranchesDialog
import com.intellij.dvcs.util.CommitCompareInfo
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.util.BackgroundTaskUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Couple
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vcs.VcsException
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.Convertor
import git4idea.DialogManager
import git4idea.GitUtil
import git4idea.changes.GitChangeUtils
import git4idea.commands.Git
import git4idea.fetch.GitFetchSupport.fetchSupport
import git4idea.history.GitHistoryUtils
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import git4idea.ui.branch.GitCompareBranchesHelper
import java.io.IOException
import java.util.*
import java.util.concurrent.Callable
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.FutureTask
import java.util.function.Predicate

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/util/GithubCreatePullRequestWorker.java
 * @author JetBrains s.r.o.
 */
class GiteeCreatePullRequestWorker(private val project: Project,
                                   private val git: Git,
                                   private val gitRepository: GitRepository,
                                   private val executor: GiteeApiRequestExecutor,
                                   private val server: GiteeServerPath,
                                   private val helper: GiteeGitHelper,
                                   private val progressManager: ProgressManager,
                                   private val path: GiteeFullPath,
                                   private val remoteName: String,
                                   private val remoteUrl: String,
                                   val currentBranch: String) {

  private lateinit var source: GiteeFullPath

  val forks: MutableList<ForkInfo> = arrayListOf()

  private var availableForks: List<GiteeFullPath>? = null

  companion object {
    private val LOG = GiteeUtil.LOG
    private const val CANNOT_CREATE_PULL_REQUEST = "Can't Create Pull Request"

    @JvmStatic
    fun create(project: Project,
               gitRepository: GitRepository,
               remote: GitRemote,
               remoteUrl: String,
               executor: GiteeApiRequestExecutor,
               server: GiteeServerPath): GiteeCreatePullRequestWorker? {

      val progressManager = ProgressManager.getInstance()

      return progressManager.runProcessWithProgressSynchronously<GiteeCreatePullRequestWorker, RuntimeException>({
        val git = ServiceManager.getService(Git::class.java)

//        gitRepository.update()
//        val remote = findGiteeRemote(server, gitRepository)
//        if (remote == null) {
//          GiteeNotifications.showError(project, CANNOT_CREATE_PULL_REQUEST, "Can't find Gitee remote")
//          return@runProcessWithProgressSynchronously null
//        }
//        val remoteName = remote.first.name
//        val remoteUrl = remote.second

        val path = GiteeUrlUtil.getUserAndRepositoryFromRemoteUrl(remoteUrl)
        if (path == null) {
          GiteeNotifications.showError(project, CANNOT_CREATE_PULL_REQUEST, "Can't process remote: $remoteUrl")
          return@runProcessWithProgressSynchronously null
        }

        val currentBranch = gitRepository.currentBranch
        if (currentBranch == null) {
          GiteeNotifications.showError(project, CANNOT_CREATE_PULL_REQUEST, "No current branch")
          return@runProcessWithProgressSynchronously null
        }

        val worker = GiteeCreatePullRequestWorker(project, git, gitRepository, executor, server,
          GiteeGitHelper.getInstance(), progressManager, path, remote.name, remoteUrl,
          currentBranch.name)

        try {
          worker.initForks(progressManager.progressIndicator)
        } catch (e: IOException) {
          GiteeNotifications.showError(project, CANNOT_CREATE_PULL_REQUEST, e)
          return@runProcessWithProgressSynchronously null
//          return@progressManager.runProcessWithProgressSynchronously null
        }

        worker
      }, "Loading Data...", true, project)
    }

//    @JvmStatic
//    internal fun findGiteeRemote(server: GiteeServerPath, repository: GitRepository): Pair<GitRemote, String>? {
//      var githubRemote: Pair<GitRemote, String>? = null
//      for (gitRemote in repository.remotes) {
//        for (remoteUrl in gitRemote.urls) {
//          if (server.matches(remoteUrl)) {
//            val remoteName = gitRemote.name
//            if ("github" == remoteName || "origin" == remoteName) {
//              return Pair.create(gitRemote, remoteUrl)
//            }
//            if (githubRemote == null) {
//              githubRemote = Pair.create(gitRemote, remoteUrl)
//            }
//            break
//          }
//        }
//      }
//      return githubRemote
//    }
  }

  @Throws(IOException::class)
  private fun initForks(indicator: ProgressIndicator) {
    doLoadForksFromGitee(indicator)
    doLoadForksFromGit(indicator)
    doLoadForksFromSettings(indicator)
  }

  private fun doAddFork(path: GiteeFullPath, remoteName: String?, indicator: ProgressIndicator) {
    for (fork in forks) {
      if (fork.path == path) {
        if (fork.remoteName == null && remoteName != null) {
          fork.remoteName = remoteName
        }
        return
      }
    }

    try {
      val branches = loadBranches(path, indicator)
      val defaultBranch = doLoadDefaultBranch(path, indicator)

      val fork = ForkInfo(path, branches, defaultBranch)
      forks.add(fork)

      if (remoteName != null) {
        fork.remoteName = remoteName
      }
    } catch (e: IOException) {
      GiteeNotifications.showWarning(project, "Can't load branches for " + path.fullName, e)
    }

  }

  private fun doAddFork(repo: GiteeRepo, indicator: ProgressIndicator): ForkInfo? {
    val path = repo.fullPath

    for (fork in forks) {
      if (fork.path == path) {
        return fork
      }
    }

    return try {
      val branches = loadBranches(path, indicator)
      val defaultBranch = repo.defaultBranch

      val fork = ForkInfo(path, branches, defaultBranch)
      forks.add(fork)
      fork
    } catch (e: IOException) {
      GiteeNotifications.showWarning(project, "Can't load branches for " + path.fullName, e)
      null
    }

  }

  private fun doLoadForksFromSettings(indicator: ProgressIndicator) {
    val savedRepo = GiteeProjectSettings.getInstance(project).createPullRequestDefaultRepo

    if (savedRepo != null) {
      doAddFork(savedRepo, null, indicator)
    }
  }

  private fun doLoadForksFromGit(indicator: ProgressIndicator) {
    for (remote in gitRepository.getRemotes()) {
      for (url in remote.getUrls()) {
        if (server.matches(url)) {
          val path = GiteeUrlUtil.getUserAndRepositoryFromRemoteUrl(url)
          if (path != null) {
            doAddFork(path, remote.getName(), indicator)
            break
          }
        }
      }
    }
  }

  @Throws(IOException::class)
  private fun doLoadForksFromGitee(indicator: ProgressIndicator) {
    val repo = executor.execute(indicator, GiteeApiRequests.Repos.get(server, path.user, path.repository))
      ?: throw GiteeConfusingException("Can't find gitee repo " + path.toString())

    doAddFork(repo, indicator)

    if (repo.parent != null) {
      doAddFork(repo.parent!!, indicator)
    }

    if (repo.source != null) {
      doAddFork(repo.source!!, indicator)
    }

    source = if (repo.source == null) repo.fullPath else repo.source!!.fullPath
  }

  @Throws(IOException::class)
  private fun loadBranches(fork: GiteeFullPath, indicator: ProgressIndicator): List<String> {
    val branches = executor.execute(indicator, GiteeApiRequests.Repos.Branches.get(server, fork.user, fork.repository))
    return branches.map { it.name }
  }

  @Throws(IOException::class)
  private fun doLoadDefaultBranch(fork: GiteeFullPath, indicator: ProgressIndicator): String? {
    val repo = executor.execute(indicator, GiteeApiRequests.Repos.get(server, fork.user, fork.repository))
      ?: throw GiteeConfusingException("Can't find gitee repo " + fork.toString())

    return repo.defaultBranch
  }

  private fun doConfigureRemote(fork: ForkInfo) {
    if (fork.remoteName != null) return

    val path = fork.path
    val url = helper.getRemoteUrl(server, path)

    try {
      git.addRemote(gitRepository, path.getUser(), url).throwOnError()
      gitRepository.update()
      fork.remoteName = path.user
    } catch (e: VcsException) {
      GiteeNotifications.showError(project, "Can't add remote", "Failed to add Gitee remote: '" + url + "'. " + e.message)
    }
  }

  fun configureRemote(fork: ForkInfo) {
    progressManager.runProcessWithProgressSynchronously(
      { doConfigureRemote(fork) },
      "Creating Remote..",
      false,
      project
    )
  }

  fun getDefaultDescriptionMessage(branch: BranchInfo): Couple<String> {
    val message = branch.defaultMessage
    if (message != null) return message

    return if (branch.forkInfo.remoteName == null) {
      getSimpleDefaultDescriptionMessage(branch)
    } else {
      progressManager.runProcessWithProgressSynchronously(
        ThrowableComputable<Couple<String>, IOException> {
          val targetBranch = branch.forkInfo.remoteName + "/" + branch.remoteName
          try {
            val commits = GitHistoryUtils.collectCommitsMetadata(project, gitRepository.root, currentBranch, targetBranch)
              ?: return@ThrowableComputable getSimpleDefaultDescriptionMessage(branch)

            val localCommit = commits[0]
            val targetCommit = commits[1]

            if (localCommit.parents.contains(targetCommit.id)) {
              return@ThrowableComputable GiteeUtil.getGiteeLikeFormattedDescriptionMessage(localCommit.fullMessage)
            }
            return@ThrowableComputable getSimpleDefaultDescriptionMessage(branch)
          } catch (e: ProcessCanceledException) {
            return@ThrowableComputable getSimpleDefaultDescriptionMessage(branch)
          } catch (e: VcsException) {
            GiteeNotifications.showWarning(project, "Can't collect additional data", e)
            return@ThrowableComputable getSimpleDefaultDescriptionMessage(branch)
          }
        },
        "Collecting Last Commits...",
        true,
        project
      )
    }

  }

  fun getSimpleDefaultDescriptionMessage(branch: BranchInfo): Couple<String> {
    val message = Couple.of(currentBranch, "")
    branch.defaultMessage = message
    return message
  }

  fun checkAction(branch: BranchInfo?): Boolean {
    if (branch == null) {
      GiteeNotifications.showWarningDialog(project, CANNOT_CREATE_PULL_REQUEST, "Target branch is not selected")
      return false
    }

    val info: DiffInfo?

    try {
      info = progressManager.runProcessWithProgressSynchronously(
        ThrowableComputable<DiffInfo?, IOException> {
          GiteeUtil.runInterruptable(
            progressManager.progressIndicator,
            ThrowableComputable<DiffInfo, IOException> { getDiffInfo(branch) }
          )
        },
        "Collecting Diff Data...",
        false,
        project
      )
    } catch (e: IOException) {
      GiteeNotifications.showError(project, "Can't collect diff data", e)
      return true
    }

    if (info == null) {
      return true
    }

    val fork = branch.forkInfo

    val localBranchName = "'$currentBranch'"
    val targetBranchName = "'" + fork.remoteName + "/" + branch.remoteName + "'"

    if (info.info.getBranchToHeadCommits(gitRepository).isEmpty()) {
      return GiteeNotifications.showYesNoDialog(project,
        "Empty Pull Request",
        "The branch " + localBranchName + " is fully merged to the branch " + targetBranchName + '\n'.toString() + "Do you want to proceed anyway?"
      )
    }
    return if (!info.info.getHeadToBranchCommits(gitRepository).isEmpty()) {
      GiteeNotifications.showYesNoDialog(project,
        "Target Branch Is Not Fully Merged",
        ("The branch " + targetBranchName + " is not fully merged to the branch " + localBranchName + '\n'.toString() + "Do you want to proceed anyway?")
      )
    } else {
      true
    }
  }

  fun launchFetchRemote(fork: ForkInfo) {
    if (fork.remoteName == null) return

    if (fork.fetchTask != null) return
    synchronized(fork.LOCK) {
      if (fork.fetchTask != null) return

      val task = MasterFutureTask(Callable<Void> {
        BackgroundTaskUtil.runUnderDisposeAwareIndicator(project, Runnable { doFetchRemote(fork) })
        null
      })
      fork.fetchTask = task

      ApplicationManager.getApplication().executeOnPooledThread(task)
    }
  }

  fun launchLoadDiffInfo(branch: BranchInfo) {
    if (branch.forkInfo.remoteName == null) return

    if (branch.diffInfoTask != null) return

    synchronized(branch.LOCK) {
      if (branch.diffInfoTask != null) return

      launchFetchRemote(branch.forkInfo)

      val masterTask = branch.forkInfo.fetchTask!!

      val task = SlaveFutureTask<DiffInfo>(masterTask, Callable { doLoadDiffInfo(branch) })
      branch.diffInfoTask = task

      ApplicationManager.getApplication().executeOnPooledThread(task)
    }
  }

  @Throws(IOException::class)
  fun getDiffInfo(branch: BranchInfo): DiffInfo? {
    if (branch.forkInfo.remoteName == null) return null

    launchLoadDiffInfo(branch)

    assert(branch.diffInfoTask != null)

    return try {
      branch.diffInfoTask!!.get()
    } catch (e: InterruptedException) {
      throw GiteeOperationCanceledException(e)
    } catch (e: ExecutionException) {
      val ex = e.cause
      if (ex is VcsException) throw IOException(ex)
      LOG.error(ex)
      null
    }
  }

  @Throws(VcsException::class)
  private fun doLoadDiffInfo(branch: BranchInfo): DiffInfo {
    // TODO: make cancelable and abort old speculative requests (when intellij.vcs.git will allow to do so)
    val targetBranch = branch.forkInfo.remoteName + "/" + branch.remoteName

    val commits1 = GitHistoryUtils.history(project, gitRepository.root, "..$targetBranch")
    val commits2 = GitHistoryUtils.history(project, gitRepository.root, "$targetBranch..")

    val diff = GitChangeUtils.getDiff(project, gitRepository.root, targetBranch, currentBranch, null)
    val info = CommitCompareInfo(CommitCompareInfo.InfoType.BRANCH_TO_HEAD)

    info.putTotalDiff(gitRepository, diff)
    info.put(gitRepository, commits1, commits2)

    return DiffInfo(info, currentBranch, targetBranch)
  }

  private fun doFetchRemote(fork: ForkInfo) {
//    if (fork.remoteName == null) return
//    val result = GitFetcher(project, EmptyProgressIndicator(), false).fetch(gitRepository.root, fork.remoteName!!, null)
//    if (!result.isSuccess) {
//      GitFetcher.displayFetchResult(project, result, null, result.errors)
//    }

    val remoteName: String? = fork.remoteName ?: return
    val remote = GitUtil.findRemoteByName(gitRepository, remoteName!!)
    if (remote == null) {
      LOG.warn("Couldn't find remote $remoteName in $gitRepository")
    }
    fetchSupport(project).fetch(gitRepository, remote!!).showNotificationIfFailed()
  }

  fun createPullRequest(branch: BranchInfo, title: String, description: String) {

    object : Task.Backgroundable(project, "Creating Pull Request...") {
      override fun run(indicator: ProgressIndicator) {
        LOG.info("Pushing current branch")
        indicator.text = "Pushing current branch..."

        val result = git.push(gitRepository, remoteName, remoteUrl, currentBranch, true)

        if (!result.success()) {
          GiteeNotifications.showError(
            this@GiteeCreatePullRequestWorker.project,
            CANNOT_CREATE_PULL_REQUEST,
            "Push failed:<br/>" + result.errorOutputAsHtmlString
          )
          return
        }

        LOG.info("Creating pull request")
        indicator.text = "Creating pull request..."
        val request = doCreatePullRequest(indicator, branch, title, description) ?: return

        GiteeNotifications.showInfoURL(
          this@GiteeCreatePullRequestWorker.project,
          "Successfully created pull request",
          "Pull request #" + request.number, request.htmlUrl
        )
      }
    }.queue()
  }

  private fun doCreatePullRequest(indicator: ProgressIndicator,
                                  branch: BranchInfo,
                                  title: String,
                                  description: String): GiteePullRequest? {

    val forkPath = branch.forkInfo.path

    val head = path.user + ":" + currentBranch
    val base = branch.remoteName

    return try {
      executor.execute(indicator, GiteeApiRequests.Repos.PullRequests.create(server, forkPath.user, forkPath.repository, title, description, head, base))
    } catch (e: IOException) {
      GiteeNotifications.showError(project, CANNOT_CREATE_PULL_REQUEST, e)
      null
    }

  }

  fun showDiffDialog(branch: BranchInfo?) {
    if (branch == null) {
      GiteeNotifications.showWarningDialog(project, "Can't Show Diff", "Target branch is not selected")
      return
    }

    val info: DiffInfo?
    try {
      info = progressManager.runProcessWithProgressSynchronously(
        ThrowableComputable<DiffInfo?, IOException> {
          GiteeUtil.runInterruptable(
            progressManager.progressIndicator,
            ThrowableComputable { getDiffInfo(branch) })
        },
        "Collecting Diff Data...",
        true,
        project
      )
    } catch (e: IOException) {
      GiteeNotifications.showError(project, "Can't collect diff data", e)
      return
    }

    if (info == null) {
      GiteeNotifications.showErrorDialog(project, "Can't Show Diff", "Can't collect diff data")
      return
    }

    val dialog = CompareBranchesDialog(GitCompareBranchesHelper(project), info.to, info.from, info.info, gitRepository, true)
    dialog.show()
  }

  fun showTargetDialog(): ForkInfo? {
    if (availableForks == null) {
      try {
        availableForks = progressManager.runProcessWithProgressSynchronously(
          ThrowableComputable<List<GiteeFullPath>?, IOException> { getAvailableForks(progressManager.progressIndicator) },
          currentBranch,
          false,
          project
        )
      } catch (ignore: ProcessCanceledException) {
      }
    }

    val getForkPath = Convertor<String, ForkInfo> { user: String ->
      progressManager.runProcessWithProgressSynchronously(
        ThrowableComputable<ForkInfo, IOException> { findRepositoryByUser(progressManager.progressIndicator, user) },
        GiteeBundle.message2("gitee.access.dialog.title"),
        false,
        project
      )
    }

    val dialog = GiteeSelectForkDialog(project, availableForks, getForkPath)
    DialogManager.show(dialog)
    return if (!dialog.isOK) {
      null
    } else dialog.getPath()
  }

  private fun getAvailableForks(indicator: ProgressIndicator): List<GiteeFullPath>? {
    return try {
      val forks = GiteeApiPagesLoader.loadAll(executor, indicator, GiteeApiRequests.Repos.Forks.pages(server, source.user, source.repository))
      val forkPaths = ContainerUtil.map(forks, GiteeRepo::getFullPath)
      if (!forkPaths.contains(source)) ContainerUtil.append(forkPaths, source) else forkPaths
    } catch (e: IOException) {
      GiteeNotifications.showWarning(project, "Can't load available forks", e)
      null
    }

  }

  private fun findRepositoryByUser(indicator: ProgressIndicator, user: String): ForkInfo? {
    for (fork in forks) {
      if (StringUtil.equalsIgnoreCase(user, fork.path.user)) {
        return fork
      }
    }

    try {
      val repo: GiteeRepo?
      val target = executor.execute(indicator, GiteeApiRequests.Repos.get(server, user, source.repository))

      repo = if (target != null && target.source != null && StringUtil.equals(target.source!!.userName, source.user)) {
        target
      } else {
        GiteeApiPagesLoader.find(executor, indicator, GiteeApiRequests.Repos.Forks.pages(server, source.user, source.repository),
          Predicate { fork -> StringUtil.equalsIgnoreCase(fork.userName, user) })
      }

      return if (repo == null) null else doAddFork(repo, indicator)
    } catch (e: IOException) {
      GiteeNotifications.showError(project, "Can't find repository", e)
      return null
    }

  }


  class ForkInfo(val path: GiteeFullPath, // initial loading
                 _branches: List<String>,
                 _defaultBranch: String?) {

    val LOCK = Any()

    val defaultBranch: String = _defaultBranch ?: "master"

    val branches: MutableList<BranchInfo> = _branches.mapTo(ArrayList()) { BranchInfo(it, this) }

    var remoteName: String? = null
    var isProposedToCreateRemote: Boolean = false

    var fetchTask: MasterFutureTask<Void>? = null

//    val branches: List<BranchInfo>
//      get() = myBranches

//    init {
//      myBranches = branches.mapTo(ArrayList()) { BranchInfo(it, this) }
//    }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (other == null || javaClass != other.javaClass) return false

      val info = other as ForkInfo?

      return path == info!!.path
    }

    override fun hashCode(): Int {
      return path.hashCode()
    }

    override fun toString(): String {
      return path.user + ":" + path.repository
    }
  }

  class BranchInfo(val remoteName: String,
                   val forkInfo: ForkInfo) {

    val LOCK = Any()

    var diffInfoTask: SlaveFutureTask<DiffInfo>? = null
    var defaultMessage: Couple<String>? = null

    override fun toString(): String {
      return remoteName
    }
  }

  class DiffInfo internal constructor(val info: CommitCompareInfo,
                                      val from: String,  // HEAD
                                      val to: String)    // BASE

  class SlaveFutureTask<T>(private val master: MasterFutureTask<*>,
                           callable: Callable<T>) : FutureTask<T>(callable) {

    override fun run() {
      if (master.isDone) {
        super.run()
      } else {
        if (!master.addSlave(this)) {
          super.run()
        }
      }
    }

    fun safeGet(): T? {
      return try {
        super.get()
      } catch (e: InterruptedException) {
        null
      } catch (e: ExecutionException) {
        null
      } catch (e: CancellationException) {
        null
      }

    }
  }

  class MasterFutureTask<T>(callable: Callable<T>) : FutureTask<T>(callable) {
    private val LOCK = Any()
    private var done = false

    private var slaves: MutableList<SlaveFutureTask<*>>? = null

    internal fun addSlave(slave: SlaveFutureTask<*>): Boolean {
      return if (isDone) {
        false
      } else {
        synchronized(LOCK) {
          if (done) return false
          if (slaves == null) slaves = arrayListOf()
          slaves!!.add(slave)
          return true
        }
      }
    }

    override fun done() {
      synchronized(LOCK) {
        done = true
        if (slaves != null) {
          for (slave in slaves!!) {
            runSlave(slave)
          }
          slaves = null
        }
      }
    }

    protected fun runSlave(slave: SlaveFutureTask<*>) {
      ApplicationManager.getApplication().executeOnPooledThread(slave)
    }
  }

}