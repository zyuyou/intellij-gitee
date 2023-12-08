package com.gitee.actions

import com.gitee.util.GEHostedRepositoriesManager
import com.intellij.openapi.components.service
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import git4idea.remote.hosting.HostedGitRepositoriesManager
import git4idea.remote.hosting.action.GlobalHostedGitRepositoryReferenceActionGroup
import git4idea.remote.hosting.action.HostedGitRepositoryReference
import java.awt.datatransfer.StringSelection
import java.net.URI

class GECopyLinkActionGroup : GlobalHostedGitRepositoryReferenceActionGroup() {
    override fun repositoriesManager(project: Project): HostedGitRepositoriesManager<*> =
        project.service<GEHostedRepositoriesManager>()

    override fun getUri(repository: URI, revisionHash: String): URI =
        GEPathUtil.getWebURI(repository, revisionHash)

    override fun getUri(repository: URI, revisionHash: String, relativePath: String, lineRange: IntRange?): URI =
        GEPathUtil.getWebURI(repository, revisionHash, relativePath, lineRange)

    override fun handleReference(reference: HostedGitRepositoryReference) {
        val uri = reference.buildWebURI()?.toString() ?: return
        CopyPasteManager.getInstance().setContents(StringSelection(uri))
    }
}