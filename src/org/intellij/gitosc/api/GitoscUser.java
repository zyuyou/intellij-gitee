package org.intellij.gitosc.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by zyuyou on 16/5/25.
 */
public class GitoscUser {
	@NotNull private final String myLogin;
	@NotNull private final String myHtmlUrl;
	@Nullable private final String myAvatarUrl;

	public GitoscUser(@NotNull String myLogin, @NotNull String myHtmlUrl, @Nullable String myAvatarUrl) {
		this.myLogin = myLogin;
		this.myHtmlUrl = myHtmlUrl;
		this.myAvatarUrl = myAvatarUrl;
	}

	@NotNull
	public String getLogin() {
		return myLogin;
	}

	@NotNull
	public String getHtmlUrl() {
		return myHtmlUrl;
	}

	@NotNull
	public String getAvatarUrl() {
		return myAvatarUrl;
	}
}
