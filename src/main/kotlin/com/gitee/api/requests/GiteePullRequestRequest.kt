package com.gitee.api.requests

/**
 * Created by zyuyou on 2018/8/14.
 *
 */
data class GiteePullRequestRequest(private val owner: String,
                                   private val title: String,
                                   private val body: String,
                                   private val head: String,    // branch with changes
                                   private val bese: String) {  // branch requested to

}