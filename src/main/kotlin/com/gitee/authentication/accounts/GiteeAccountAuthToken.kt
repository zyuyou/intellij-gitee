package com.gitee.authentication.accounts

/**
 * Created by zyuyou on 2018/8/2.
 *
 */
data class GiteeAccountAuthToken(val login: String,
                                 val accessToken: String,
                                 val refreshToken: String = "")