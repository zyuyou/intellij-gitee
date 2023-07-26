package com.gitee.api

import com.intellij.collaboration.util.resolveRelative
import git4idea.remote.hosting.HostedRepositoryCoordinates
import java.net.URI

data class GERepositoryCoordinates(override val serverPath: GiteeServerPath,
                                   val repositoryPath: GERepositoryPath)
  : HostedRepositoryCoordinates {

  fun toUrl(): String {
    return serverPath.toUrl() + "/" + repositoryPath
  }

  override fun getWebURI(): URI = serverPath.toURI().resolveRelative(repositoryPath.toString())

  override fun toString(): String {
    return "$serverPath/$repositoryPath"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is GERepositoryCoordinates) return false

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