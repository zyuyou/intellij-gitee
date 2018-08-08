// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.gitee.actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.VcsDataKeys
import com.intellij.openapi.vcs.changes.Change
import com.intellij.openapi.vcs.changes.ChangeListManager
import com.intellij.openapi.vcs.history.VcsFileRevision
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.containers.ContainerUtil
import com.intellij.vcs.log.VcsLog
import com.intellij.vcs.log.VcsLogDataKeys
import com.intellij.vcsUtil.VcsUtil
import git4idea.GitFileRevision
import git4idea.GitRevisionNumber
import git4idea.GitUtil
import git4idea.history.GitHistoryUtils
import com.gitee.api.GiteeRepositoryPath
import com.gitee.icons.GiteeIcons
import com.gitee.util.GiteeGitHelper
import com.gitee.util.GiteeNotifications
import com.gitee.util.GiteeUtil

open class GiteeOpenInBrowserActionGroup : ActionGroup("Open on Gitee", "Open corresponding link in browser", com.gitee.icons.GiteeIcons.Gitee_icon) {

  override fun update(e: AnActionEvent) {
    val repositories = getData(e.dataContext)?.first
    e.presentation.isEnabledAndVisible = repositories != null && repositories.isNotEmpty()
  }

  override fun getChildren(e: AnActionEvent?): Array<AnAction> {
    if (e != null) {
      val data = getData(e.dataContext)
      if (data != null && data.first.size > 1) {
        return data.first.map { com.gitee.actions.GiteeOpenInBrowserActionGroup.Companion.GiteeOpenInBrowserAction(it, data.second) }.toTypedArray()
      }
    }
    return emptyArray()
  }

  override fun isPopup(): Boolean = true

  override fun actionPerformed(e: AnActionEvent) {
    getData(e.dataContext)?.let { com.gitee.actions.GiteeOpenInBrowserActionGroup.Companion.GiteeOpenInBrowserAction(it.first.first(), it.second) }?.actionPerformed(e)
  }

  override fun canBePerformed(context: DataContext?): Boolean {
    if (context == null) return false

    val data = getData(context)
    return data != null && data.first.size == 1
  }

  override fun disableIfNoVisibleChildren(): Boolean = false

  protected open fun getData(dataContext: DataContext): Pair<Set<GiteeRepositoryPath>, com.gitee.actions.GiteeOpenInBrowserActionGroup.Data>? {
    val project = dataContext.getData(CommonDataKeys.PROJECT) ?: return null

    return getDataFromHistory(project, dataContext.getData(VcsDataKeys.FILE_PATH), dataContext.getData(VcsDataKeys.VCS_FILE_REVISION))
           ?: getDataFromLog(project, dataContext.getData(VcsLogDataKeys.VCS_LOG))
           ?: getDataFromVirtualFile(project, dataContext.getData(CommonDataKeys.VIRTUAL_FILE))
  }

  private fun getDataFromHistory(project: Project, filePath: FilePath?, fileRevision: VcsFileRevision?): Pair<Set<GiteeRepositoryPath>, com.gitee.actions.GiteeOpenInBrowserActionGroup.Data>? {
    if (filePath == null || fileRevision == null || fileRevision !is GitFileRevision) return null

    val repository = GitUtil.getRepositoryManager(project).getRepositoryForFile(filePath) ?: return null

    val accessibleRepositories = service<GiteeGitHelper>().getPossibleRepositories(repository)
    if (accessibleRepositories.isEmpty()) return null

    return accessibleRepositories to com.gitee.actions.GiteeOpenInBrowserActionGroup.Data.Revision(project, fileRevision.revisionNumber.asString())
  }

  private fun getDataFromLog(project: Project, log: VcsLog?): Pair<Set<GiteeRepositoryPath>, com.gitee.actions.GiteeOpenInBrowserActionGroup.Data>? {
    if (log == null) return null

    val selectedCommits = log.selectedCommits
    if (selectedCommits.size != 1) return null

    val commit = ContainerUtil.getFirstItem(selectedCommits) ?: return null

    val repository = GitUtil.getRepositoryManager(project).getRepositoryForRoot(commit.root) ?: return null

    val accessibleRepositories = service<GiteeGitHelper>().getPossibleRepositories(repository)
    if (accessibleRepositories.isEmpty()) return null

    return accessibleRepositories to com.gitee.actions.GiteeOpenInBrowserActionGroup.Data.Revision(project, commit.hash.asString())
  }

  private fun getDataFromVirtualFile(project: Project, virtualFile: VirtualFile?): Pair<Set<GiteeRepositoryPath>, com.gitee.actions.GiteeOpenInBrowserActionGroup.Data>? {
    if (virtualFile == null) return null

    val repository = GitUtil.getRepositoryManager(project).getRepositoryForFile(virtualFile) ?: return null

    val accessibleRepositories = service<GiteeGitHelper>().getPossibleRepositories(repository)
    if (accessibleRepositories.isEmpty()) return null

    val changeListManager = ChangeListManager.getInstance(project)
    if (changeListManager.isUnversioned(virtualFile)) return null

    val change = changeListManager.getChange(virtualFile)

    return if (change != null && change.type == Change.Type.NEW) null
    else accessibleRepositories to com.gitee.actions.GiteeOpenInBrowserActionGroup.Data.File(project, repository.root, virtualFile)
  }

  protected sealed class Data(val project: Project) {
    class File(project: Project, val gitRepoRoot: VirtualFile, val virtualFile: VirtualFile) : com.gitee.actions.GiteeOpenInBrowserActionGroup.Data(project)

    class Revision(project: Project, val revisionHash: String) : com.gitee.actions.GiteeOpenInBrowserActionGroup.Data(project)
  }

  private companion object {
    private const val CANNOT_OPEN_IN_BROWSER = "Can't open in browser"

    class GiteeOpenInBrowserAction(private val repoPath: GiteeRepositoryPath, val data: com.gitee.actions.GiteeOpenInBrowserActionGroup.Data)
      : AnAction(repoPath.toString().replace('_', ' ')) {

      override fun actionPerformed(e: AnActionEvent) {
        when (data) {
          is com.gitee.actions.GiteeOpenInBrowserActionGroup.Data.Revision -> openCommitInBrowser(repoPath, data.revisionHash)
          is com.gitee.actions.GiteeOpenInBrowserActionGroup.Data.File -> openFileInBrowser(data.project, data.gitRepoRoot, repoPath, data.virtualFile, e.getData(CommonDataKeys.EDITOR))
        }
      }

      private fun openCommitInBrowser(path: GiteeRepositoryPath, revisionHash: String) {
        BrowserUtil.browse("${path.toUrl()}/commit/$revisionHash")
      }

      private fun openFileInBrowser(project: Project,
                                    repositoryRoot: VirtualFile,
                                    path: GiteeRepositoryPath,
                                    virtualFile: VirtualFile,
                                    editor: Editor?) {

        val relativePath = VfsUtilCore.getRelativePath(virtualFile, repositoryRoot)

        if (relativePath == null) {
          com.gitee.util.GiteeNotifications.showError(project, com.gitee.actions.GiteeOpenInBrowserActionGroup.Companion.CANNOT_OPEN_IN_BROWSER, "File is not under repository root",
                                        "Root: " + repositoryRoot.presentableUrl + ", file: " + virtualFile.presentableUrl)
          return
        }

        val hash = getCurrentFileRevisionHash(project, virtualFile)
        if (hash == null) {
          com.gitee.util.GiteeNotifications.showError(project, com.gitee.actions.GiteeOpenInBrowserActionGroup.Companion.CANNOT_OPEN_IN_BROWSER, "Can't get last revision.")
          return
        }

        val giteeUrl = makeUrlToOpen(editor, relativePath, hash, path)
        if (giteeUrl != null) BrowserUtil.browse(giteeUrl)
      }

      private fun getCurrentFileRevisionHash(project: Project, file: VirtualFile): String? {
        val ref = Ref<GitRevisionNumber>()
        object : Task.Modal(project, "Getting Last Revision", true) {
          override fun run(indicator: ProgressIndicator) {
            ref.set(GitHistoryUtils.getCurrentRevision(project, VcsUtil.getFilePath(file), "HEAD") as GitRevisionNumber?)
          }

          override fun onThrowable(error: Throwable) {
            GiteeUtil.LOG.warn(error)
          }
        }.queue()
        return if (ref.isNull) null else ref.get().rev
      }

      private fun makeUrlToOpen(editor: Editor?,
                                relativePath: String,
                                branch: String,
                                path: GiteeRepositoryPath): String? {
        val builder = StringBuilder()

        if (StringUtil.isEmptyOrSpaces(relativePath)) {
          builder.append(path.toUrl()).append("/tree/").append(branch)
        }
        else {
          builder.append(path.toUrl()).append("/blob/").append(branch).append('/').append(relativePath)
        }

        if (editor != null && editor.document.lineCount >= 1) {
          // lines are counted internally from 0, but from 1 on gitee
          val selectionModel = editor.selectionModel
          val begin = editor.document.getLineNumber(selectionModel.selectionStart) + 1
          val selectionEnd = selectionModel.selectionEnd
          var end = editor.document.getLineNumber(selectionEnd) + 1
          if (editor.document.getLineStartOffset(end - 1) == selectionEnd) {
            end -= 1
          }
          builder.append("#L").append(begin)
          if (begin != end) {
            builder.append("-L").append(end)
          }
        }
        return builder.toString()
      }
    }
  }
}