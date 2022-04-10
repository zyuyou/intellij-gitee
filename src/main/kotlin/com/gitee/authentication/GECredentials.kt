package com.gitee.authentication

import com.intellij.collaboration.auth.credentials.CredentialsWithRefresh

class GECredentials(
  override val accessToken: String,
  override val refreshToken: String,
  override val expiresIn: Long,
  val tokenType: String,
  val scope: String,
  val createdAt: Long
) : CredentialsWithRefresh {

  override fun isAccessTokenValid(): Boolean {
    return System.currentTimeMillis() < (createdAt + expiresIn) * 1000
  }

  companion object {
    private const val _empty = ""
    val EmptyCredentials = GECredentials(_empty, _empty, 0, _empty, _empty, 0)

    const val scope: String = "user_info projects pull_requests gists issues notes groups"

    fun createCredentials(accessToken: String, refreshToken: String) =
      GECredentials(accessToken, refreshToken, 86400, "bearer", scope, System.currentTimeMillis() / 1000)
  }
}