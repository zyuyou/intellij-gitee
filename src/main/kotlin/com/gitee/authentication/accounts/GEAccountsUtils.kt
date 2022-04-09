package com.gitee.authentication.accounts

internal object GEAccountsUtils {
  data class GEAppCredentials(val clientId: String, val clientSecret: String)

  private const val APP_CLIENT_ID: String = "b7837ec65bcb294b0e2a31e5669b788a3185829524af4d818c3c2a35b186366d"
  private const val APP_CLIENT_SECRET: String = "85891acdb745502e19e02e3bbcd405dd303190c8a3fcf29a6ca7a2796b76f918"

  fun getDefaultGEAppCredentials(): GEAppCredentials {
    return GEAppCredentials(APP_CLIENT_ID, APP_CLIENT_SECRET)
  }
}