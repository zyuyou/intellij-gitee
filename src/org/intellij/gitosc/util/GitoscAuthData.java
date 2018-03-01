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
package org.intellij.gitosc.util;

import com.intellij.openapi.util.text.StringUtil;
import org.intellij.gitosc.GitoscConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/util/GithubAuthData.java
 * @author JetBrains s.r.o.
 * @author Aleksey Pivovarov
 */
public class GitoscAuthData {
	public enum AuthType {
		SESSION, BASIC, TOKEN, ANONYMOUS
	}

	@NotNull private final AuthType myAuthType;
	@NotNull private final String myHost;
	@Nullable private final BasicAuth myBasicAuth;
	@Nullable private final TokenAuth myTokenAuth;
	@Nullable private final SessionAuth mySessionAuth;

	private final boolean myUseProxy;

	private GitoscAuthData(@NotNull AuthType authType,
	                       @NotNull String host,
	                       @Nullable BasicAuth basicAuth,
	                       @Nullable TokenAuth tokenAuth,
	                       @Nullable SessionAuth sessionAuth,
	                       boolean useProxy) {
		myAuthType = authType;
		myHost = host;
		myBasicAuth = basicAuth;
		myTokenAuth = tokenAuth;
		mySessionAuth = sessionAuth;
		myUseProxy = useProxy;
	}

	@NotNull
	public AuthType getAuthType() {
		return myAuthType;
	}

	@NotNull
	public String getHost() {
		return myHost;
	}

	@Nullable
	public BasicAuth getBasicAuth() {
		return myBasicAuth;
	}

	@Nullable
	public TokenAuth getTokenAuth() {
		return myTokenAuth;
	}

	@Nullable
	public SessionAuth getSessionAuth() {
		return mySessionAuth;
	}

	public boolean isUseProxy() {
		return myUseProxy;
	}

	//============================================================
	// create auths
	//============================================================
	public static GitoscAuthData createFromSettings(){
		return GitoscSettings.getInstance().getAuthData();
	}

	public static GitoscAuthData createAnonymous(){
		return createAnonymous(GitoscConstants.DEFAULT_GITOSC_HOST);
	}

	public static GitoscAuthData createAnonymous(@NotNull String host){
		return new GitoscAuthData(AuthType.ANONYMOUS, host, null, null, null, true);
	}

	public static GitoscAuthData createSessionAuth(@NotNull String host, @NotNull String login, @NotNull String password) {
		return new GitoscAuthData(AuthType.SESSION, host, null, null, new SessionAuth(login, password, null), true);
	}

	public static GitoscAuthData createSessionAuth(@NotNull String host, @NotNull String login, @NotNull String password, @Nullable String accessToken) {
		return new GitoscAuthData(AuthType.SESSION, host, null, null, new SessionAuth(login, password, accessToken), true);
	}

	public static GitoscAuthData createTokenAuth(@NotNull String host, @NotNull String token) {
		return new GitoscAuthData(AuthType.TOKEN, host, null, new TokenAuth(token), null,true);
	}

	public static GitoscAuthData createTokenAuth(@NotNull String host, @NotNull String token, @NotNull String refreshToken) {
		return new GitoscAuthData(AuthType.TOKEN, host, null, new TokenAuth(token, refreshToken), null,true);
	}

	public static GitoscAuthData createTokenAuth(@NotNull String host, @NotNull String token, boolean useProxy) {
		return new GitoscAuthData(AuthType.TOKEN, host, null, new TokenAuth(token), null, useProxy);
	}

	public static GitoscAuthData createTokenAuth(@NotNull String host, @NotNull String token, @NotNull String refreshToken, boolean useProxy) {
		return new GitoscAuthData(AuthType.TOKEN, host, null, new TokenAuth(token, refreshToken), null, useProxy);
	}

	//============================================================
	// static classes
	//============================================================

	public static class BasicAuth {
		@NotNull private final String myLogin;
		@NotNull private final String myPassword;
		@Nullable private final String myCode;

		private BasicAuth(@NotNull String login, @NotNull String password) {
			this(login, password, null);
		}

		private BasicAuth(@NotNull String login, @NotNull String password, @Nullable String code) {
			myLogin = login;
			myPassword = password;
			myCode = code;
		}

		@NotNull
		public String getLogin() {
			return myLogin;
		}

		@NotNull
		public String getPassword() {
			return myPassword;
		}

		@Nullable
		public String getCode() {
			return myCode;
		}
	}

	public static class TokenAuth {
		@NotNull private final String myToken;
		@NotNull private final String myRefreshToken;

		@NotNull private boolean myTryRefreshAccessToken = true;

		private TokenAuth(@NotNull String token) {
			myToken = StringUtil.trim(token);
			myRefreshToken = "";
		}

		private TokenAuth(@NotNull String token, @NotNull String refreshToken) {
			myToken = StringUtil.trim(token);
			myRefreshToken = StringUtil.trim(refreshToken);
		}

		@NotNull
		public String getToken() {
			return myToken;
		}

		@NotNull
		public String getRefreshToken() {
			return myRefreshToken;
		}

		@NotNull
		public boolean isTryRefreshAccessToken() {
			return !StringUtil.isEmptyOrSpaces(myToken) && !StringUtil.isEmptyOrSpaces(myRefreshToken) && myTryRefreshAccessToken;
		}

		public void setTryRefreshAccessToken(@NotNull boolean myTryRefreshAccessToken) {
			this.myTryRefreshAccessToken = myTryRefreshAccessToken;
		}
	}

	public static class SessionAuth {
		@NotNull private final String myLogin;
		@NotNull private final String myPassword;

		@Nullable private String myAccessToken;

		@NotNull private boolean myTryGetNewAccessToken = true;

		private SessionAuth(@NotNull String login, @NotNull String password, @Nullable String accessToken) {
			myLogin = login;
			myPassword = password;
			myAccessToken = accessToken;
		}

		@NotNull
		public String getLogin() {
			return myLogin;
		}

		@NotNull
		public String getPassword() {
			return myPassword;
		}

		@Nullable
		public String getAccessToken() {
			return myAccessToken;
		}

		private void setAccessToken(@NotNull String myAccessToken) {
			this.myAccessToken = myAccessToken;
		}

		@NotNull
		public boolean isTryGetNewAccessToken() {
			return StringUtil.isEmptyOrSpaces(myLogin) && StringUtil.isEmptyOrSpaces(myPassword) && myTryGetNewAccessToken;
		}

		public void setTryGetNewAccessToken(@NotNull boolean myTryGetNewAccessToken) {
			this.myTryGetNewAccessToken = myTryGetNewAccessToken;
		}
	}
}
