package com.gitee.api

data class GiteeRepositoryCoordinates(val serverPath: GiteeServerPath, val repositoryPath: GiteeRepositoryPath) {
    fun toUrl(): String {
        return serverPath.toUrl() + "/" + repositoryPath
    }

    override fun toString(): String {
        return "$serverPath/$repositoryPath"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is GiteeRepositoryCoordinates) return false

        if (serverPath != other.serverPath) return false
        if (repositoryPath != other.repositoryPath) return false

        return true
    }

    override fun hashCode(): Int {
        var result = serverPath.hashCode()
        result = 31 * result + repositoryPath.hashCode()
        return result
    }
}