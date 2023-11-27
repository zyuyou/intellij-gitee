package com.gitee.actions

import com.gitee.i18n.GiteeBundle
import com.gitee.icons.GiteeIcons
import com.gitee.util.GEHostedRepositoriesManager
import com.intellij.collaboration.util.resolveRelative
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.io.URLUtil
import com.intellij.util.withFragment
import git4idea.remote.hosting.HostedGitRepositoriesManager
import git4idea.remote.hosting.action.GlobalHostedGitRepositoryReferenceActionGroup
import git4idea.remote.hosting.action.HostedGitRepositoryReference
import java.net.URI

class GEOpenInBrowserActionGroup
    : GlobalHostedGitRepositoryReferenceActionGroup(
    GiteeBundle.messagePointer("open.on.gitee.action"),
    GiteeBundle.messagePointer("open.on.gitee.action.description"),
    { GiteeIcons.Gitee_icon }) {

    override fun repositoriesManager(project: Project): HostedGitRepositoriesManager<*> {
        return project.service<GEHostedRepositoriesManager>()
    }

    override fun getUri(repository: URI, revisionHash: String): URI =
        GEPathUtil.getWebURI(repository, revisionHash)

    override fun getUri(repository: URI, revisionHash: String, relativePath: String, lineRange: IntRange?): URI =
        GEPathUtil.getWebURI(repository, revisionHash, relativePath, lineRange)

    override fun handleReference(reference: HostedGitRepositoryReference) {
        val uri = reference.buildWebURI() ?: return
        BrowserUtil.browse(uri)
    }
}

object GEPathUtil {
    fun getWebURI(repository: URI, revisionOrBranch: String): URI =
        repository.resolveRelative("commit").resolveRelative(revisionOrBranch)

    fun getWebURI(repository: URI, revisionOrBranch: String, relativePath: String, lineRange: IntRange?): URI {
        val fileUri = repository.resolveRelative("blob").resolveRelative(revisionOrBranch).resolveRelative(URLUtil.encodePath(relativePath))

        return if (lineRange != null) {
            val fragmentBuilder = StringBuilder()
            fragmentBuilder.append("L").append(lineRange.first + 1)
            if (lineRange.last != lineRange.first) {
                fragmentBuilder.append("-L").append(lineRange.last + 1)
            }
            fileUri.withFragment(fragmentBuilder.toString())
        }
        else {
            fileUri
        }
    }
}