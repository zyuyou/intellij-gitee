/*
 *  Copyright 2016-2019 码云 - Gitee
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.gitee.api.data;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.jetbrains.annotations.NotNull;

import java.util.Date;

@SuppressWarnings("UnusedDeclaration")
public class GiteeIssueComment {
	private Long id;

	private String url;
	private String body;

	@JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ssXXX", locale = "zh", timezone = "GMT+8")
	private Date createdAt;
	private GiteeUser user;

	public long getId() {
		return id;
	}

	@NotNull
	public String getBody() {
		return body;
	}

	@NotNull
	public Date getCreatedAt() {
		return createdAt;
	}

	@NotNull
	public GiteeUser getUser() {
		return user;
	}
}
