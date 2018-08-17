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

import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.mandatory.RestModel;

@RestModel
@SuppressWarnings("UnusedDeclaration")
public class GiteeUserDetailed extends GiteeUser {
	private String name;
	private String email;
	private String type;
	private String weibo;
	private String bio;

	private String privateToken;

	private Integer publicRepos;
	private Integer publicGists;

	private Integer privateGists;

	private UserAddress address;

	@RestModel
	public static class UserAddress {
		private String name;
		private String tel;
		private String address;
		private String province;
		private String city;
		private String zipCode;
		private String comment;
	}

	@Nullable
	public String getName() {
		return name;
	}

	@Nullable
	public String getEmail() {
		return email;
	}

	// v3 session api
//	@Nullable public Long id;
//	@Nullable public String username;
//	@Nullable public String name;
//	@Nullable public String bio;
//	@Nullable public String weibo;
//	@Nullable public String blog;
	private Integer themeId;
	private String state;
//	@Nullable public Date createdAt;
	private String portrait;
//	@Nullable public String email;
	private String newPortrait;
	private Follow follow;
//	@Nullable public String privateToken;
	private Boolean isAdmin;
	private Boolean canCreateGroup;
	private Boolean canCreateProject;
	private Boolean canCreateTeam;

	@Nullable
	public String getPrivateToken() {
		return privateToken;
	}

	@RestModel
	public static class Follow{
		private Long followers;
		private Long starred;
		private Long following;
		private Long watched;

		public Long getFollowers() {
			return followers;
		}
		public Long getStarred() {
			return starred;
		}
		public Long getFollowing() {
			return following;
		}
		public Long getWatched() {
			return watched;
		}
	}

//	@Nullable private final String myName;
//	@Nullable private final String myEmail;
//	@Nullable private final String myPrivateToken;

//	public GiteeUserDetailed(@NotNull String myLogin,
//	                          @NotNull String myHtmlUrl,
//	                          @Nullable String myAvatarUrl,
//	                          @Nullable String name,
//	                          @Nullable String email,
//	                          @Nullable String privateToken) {
//		super(myLogin, myHtmlUrl, myAvatarUrl);
//
//		myName = name;
//		myEmail = email;
//		myPrivateToken = privateToken;
//	}

}
