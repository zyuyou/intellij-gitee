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
}