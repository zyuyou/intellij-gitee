/*
 * Copyright 2016-2018 码云 - Gitee
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gitee.api.data

import org.jetbrains.io.mandatory.Mandatory
import java.util.*

/**
 * @author Yuyou Chow
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