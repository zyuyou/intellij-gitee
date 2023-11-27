package com.gitee.actions

import com.gitee.i18n.GiteeBundle
import com.gitee.icons.GiteeIcons
import com.gitee.util.GEHostedRepositoriesManager
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.components.service
import com.intellij.openapi.vcs.actions.ShowAnnotateOperationsPopup
import com.intellij.openapi.vcs.annotate.FileAnnotation
import com.intellij.util.asSafely
import git4idea.GitRevisionNumber
import git4idea.annotate.GitFileAnnotation
import git4idea.remote.hosting.action.HostedGitRepositoryReference
import git4idea.remote.hosting.action.HostedGitRepositoryReferenceActionGroup
import git4idea.remote.hosting.action.HostedGitRepositoryReferenceUtil

internal class GEOpenInBrowserFromAnnotationActionGroup(val annotation: FileAnnotation)
    : HostedGitRepositoryReferenceActionGroup(
    GiteeBundle.messagePointer("open.on.gitee.action"),
    GiteeBundle.messagePointer("open.on.gitee.action.description"),
    { GiteeIcons.Gitee_icon }) {
    override fun findReferences(dataContext: DataContext): List<HostedGitRepositoryReference> {
        if (annotation !is GitFileAnnotation) return emptyList()
        val project = annotation.project
        val virtualFile = annotation.file

        val revision = ShowAnnotateOperationsPopup.getAnnotationLineNumber(dataContext).takeIf { it >= 0 }?.let {
            annotation.getLineRevisionNumber(it)
        }?.asSafely<GitRevisionNumber>() ?: return emptyList()

        return HostedGitRepositoryReferenceUtil
            .findReferences(project, project.service<GEHostedRepositoriesManager>(), virtualFile, revision, GEPathUtil::getWebURI)
    }

    override fun handleReference(reference: HostedGitRepositoryReference) {
        val uri = reference.buildWebURI() ?: return
        BrowserUtil.browse(uri)
    }
}