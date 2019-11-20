package com.gitee.api

data class GiteeRepositoryCoordinates(internal val serverPath: GiteeServerPath, internal val repositoryPath: GiteeRepositoryPath) {
    fun toUrl(): String {
        return serverPath.toUrl() + "/" + repositoryPath
    }

    override fun toString(): String {
        return "$serverPath/$repositoryPath"
    }
}