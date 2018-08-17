package com.gitee.api.requests

/**
 * Created by zyuyou on 2018/8/14.
 *
 */
data class GiteePullRequestRequest(private val owner: String,
                                   private val repo: String,
                                   private val title: String,
                                   private val body: String,
                                   private val head: String,    // branch with changes
                                   private val base: String)   // branch requested to