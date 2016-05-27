package org.intellij.gitosc.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by zyuyou on 16/5/25.
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
