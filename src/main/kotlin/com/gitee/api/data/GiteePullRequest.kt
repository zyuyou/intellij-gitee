package com.gitee.api.data

import org.jetbrains.io.mandatory.Mandatory
import java.util.*

/**
 * Created by zyuyou on 2018/8/14.
 *
 */
data class GiteePullRequest(@Mandatory val number: String,
                            @Mandatory val state: String,
                            @Mandatory val title: String,
                            val body: String,
                            val url: String,
                            @Mandatory val htmlUrl: String,
                            @Mandatory val diffUrl: String,
                            @Mandatory val patchUrl: String,
                            @Mandatory val issueUrl: String,
                            @Mandatory val createdAt: Date,
                            @Mandatory val updatedAt: Date,
                            val closedAt: Date,
                            val mergedAt: Date,
                            @Mandatory val user: GiteeUser,
                            @Mandatory val head: Link,
                            @Mandatory val base: Link) {

  class Link(@Mandatory val label: String,
             @Mandatory val ref: String,
             @Mandatory val sha: String,
             val repo: GiteeRepo,
             @Mandatory val user: GiteeUser) {

  }
}