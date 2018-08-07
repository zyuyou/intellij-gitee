/*
 * Copyright 2016-2017 码云
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
 *
 */
package com.gitee.api.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.io.mandatory.Mandatory;
import org.jetbrains.io.mandatory.RestModel;

import java.util.Date;

@RestModel
@SuppressWarnings("UnusedDeclaration")
public class GiteeIssueComment {
	@Mandatory
	private Long id;

	private String url;
//	@Mandatory
//	private String htmlUrl;
	private String body;
//	@Mandatory
//	private String bodyHtml;

	@Mandatory
	private Date createdAt;
//	@Mandatory
//	private Date updatedAt;

	@Mandatory
	private GiteeUser user;

	public long getId() {
		return id;
	}

//	@NotNull
//	public String getHtmlUrl() {
//		return htmlUrl;
//	}

//	@NotNull
//	public String getBodyHtml() {
//		return bodyHtml;
//	}

	@NotNull
	public String getBody() {
		return body;
	}

	@NotNull
	public Date getCreatedAt() {
		return createdAt;
	}

//	@NotNull
//	public Date getUpdatedAt() {
//		return updatedAt;
//	}

	@NotNull
	public GiteeUser getUser() {
		return user;
	}
}
