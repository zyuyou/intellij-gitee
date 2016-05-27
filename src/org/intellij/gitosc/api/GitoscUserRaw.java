package org.intellij.gitosc.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Date;

@SuppressWarnings("UnusedDeclaration")
public class GitoscUserRaw implements DataConstructor {
	@Nullable public Long id;
	@Nullable public String username;
	@Nullable public String name;
	@Nullable public String bio;
	@Nullable public String weibo;
	@Nullable public String blog;
	@Nullable public Integer themeId;
	@Nullable public String state;
	@Nullable public Date createdAt;
	@Nullable public String portrait;
	@Nullable public String email;
	@Nullable public String newPortrait;
	@Nullable public FollowRaw follow;
	@Nullable public String privateToken;
	@Nullable public Boolean isAdmin;
	@Nullable public Boolean canCreateGroup;
	@Nullable public Boolean canCreateProject;
	@Nullable public Boolean canCreateTeam;

	@SuppressWarnings("unchecked")
	@NotNull
	@Override
	public <T> T create(@NotNull Class<T> resultClass){
		if(resultClass == GitoscUser.class){
			return (T)createUser();
		}
		if(resultClass == GitoscUserDetailed.class){
			return (T)createUserDetailed();
		}

		throw new ClassCastException(this.getClass().getName() + ": bad class type: " + resultClass.getName());
	}


	@SuppressWarnings("ConstantConditions")
	@NotNull
	public GitoscUser createUser() {
		return new GitoscUser(username, "", newPortrait);
	}

	@SuppressWarnings("ConstantConditions")
	@NotNull
	public GitoscUser createUserDetailed() {
		return new GitoscUserDetailed(username, "", newPortrait, name, email, privateToken);
	}

	public static class FollowRaw {
		@Nullable public Long followers;
		@Nullable public Long starred;
		@Nullable public Long following;
		@Nullable public Long watched;

		@SuppressWarnings("ConstantConditions")
		@NotNull
		public GitoscUserDetailed.Follow create() {
			return new GitoscUserDetailed.Follow(followers, starred, following, watched);
		}
	}

}
