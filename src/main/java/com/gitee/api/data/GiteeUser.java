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
package com.gitee.api.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.mandatory.Mandatory;
import org.jetbrains.io.mandatory.RestModel;

import java.util.Date;

@RestModel
@SuppressWarnings("UnusedDeclaration")
public class GiteeUser {
	@Mandatory
	private String login;
	private Long id;

	private String url;
	@Mandatory
	private String htmlUrl;

	private Integer followers;
	private Integer following;
	private String avatarUrl;
	private String blog;

	private Date createdAt;

	@NotNull
	public String getLogin() {
		// compati
		return login == null ? username : login;
	}

	@NotNull
	public String getHtmlUrl() {
		return htmlUrl;
	}

	@Nullable
	public String getAvatarUrl() {
		return avatarUrl;
	}

	// v3 session api
	private String username;

//	@NotNull private final String myLogin;
//	@NotNull private final String myHtmlUrl;
//	@Nullable private final String myAvatarUrl;
//
//	public GiteeUser(@NotNull String myLogin, @NotNull String myHtmlUrl, @Nullable String myAvatarUrl) {
//		this.myLogin = myLogin;
//		this.myHtmlUrl = myHtmlUrl;
//		this.myAvatarUrl = myAvatarUrl;
//	}

//	@NotNull
//	public String getLogin() {
//		return myLogin;
//	}
//
//	@NotNull
//	public String getHtmlUrl() {
//		return myHtmlUrl;
//	}
//
//	@NotNull
//	public String getAvatarUrl() {
//		return myAvatarUrl;
//	}
}
