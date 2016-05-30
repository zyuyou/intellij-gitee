/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package org.intellij.gitosc.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *  https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/api/GithubUserDetailed.java
 */
public class GitoscUserDetailed extends GitoscUser{
	@Nullable private final String myName;
	@Nullable private final String myEmail;
	@Nullable private final String myPrivateToken;

	public GitoscUserDetailed(@NotNull String myLogin,
	                          @NotNull String myHtmlUrl,
	                          @Nullable String myAvatarUrl,
	                          @Nullable String name,
	                          @Nullable String email,
	                          @Nullable String privateToken) {
		super(myLogin, myHtmlUrl, myAvatarUrl);

		myName = name;
		myEmail = email;
		myPrivateToken = privateToken;
	}

	@Nullable
	public String getName() {
		return myName;
	}

	@Nullable
	public String getEmail() {
		return myEmail;
	}

	@Nullable
	public String getPrivateToken() {
		return myPrivateToken;
	}

	public static class Follow{
		private final Long followers;
		private final Long starred;
		private final Long following;
		private final Long watched;

		public Follow(long followers, long starred, long following, long watched) {
			this.followers = followers;
			this.starred = starred;
			this.following = following;
			this.watched = watched;
		}

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

}
