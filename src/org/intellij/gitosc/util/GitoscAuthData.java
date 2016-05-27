package org.intellij.gitosc.util;

import com.intellij.openapi.util.text.StringUtil;
import org.intellij.gitosc.api.GitoscApiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by zyuyou on 16/5/25.
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

	public void setAccessToken(String token){
		if(mySessionAuth != null){
			mySessionAuth.setAccessToken(token);
		}
	}

	//============================================================
	// create auths
	//============================================================
	public static GitoscAuthData createFromSettings(){
		return GitoscSettings.getInstance().getAuthData();
	}

	public static GitoscAuthData createAnonymous(){
		return createAnonymous(GitoscApiUtil.DEFAULT_GITOSC_HOST);
	}

	public static GitoscAuthData createAnonymous(@NotNull String host){
		return new GitoscAuthData(AuthType.ANONYMOUS, host, null, null, null, true);
	}

	public static GitoscAuthData createBasicAuth(@NotNull String host, @NotNull String login, @NotNull String password) {
		return new GitoscAuthData(AuthType.BASIC, host, new BasicAuth(login, password), null, null, true);
	}

	public static GitoscAuthData createBasicAuthTF(@NotNull String host,
	                                               @NotNull String login,
	                                               @NotNull String password,
	                                               @NotNull String code) {
		return new GitoscAuthData(AuthType.BASIC, host, new BasicAuth(login, password, code), null, null, true);
	}

	public static GitoscAuthData createTokenAuth(@NotNull String host, @NotNull String token) {
		return new GitoscAuthData(AuthType.TOKEN, host, null, new TokenAuth(token), null, true);
	}

	public static GitoscAuthData createTokenAuth(@NotNull String host, @NotNull String token, boolean useProxy) {
		return new GitoscAuthData(AuthType.TOKEN, host, null, new TokenAuth(token), null, useProxy);
	}

	public static GitoscAuthData createSessionAuth(@NotNull String host, @NotNull String login, @NotNull String password, @NotNull String accessToken) {
		return new GitoscAuthData(AuthType.SESSION, host, null, null, new SessionAuth(login, password, accessToken), true);
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

		private TokenAuth(@NotNull String token) {
			myToken = StringUtil.trim(token);
		}

		@NotNull
		public String getToken() {
			return myToken;
		}
	}

	public static class SessionAuth {
		@NotNull private final String myLogin;
		@NotNull private final String myPassword;

		@NotNull private String myAccessToken;

		private SessionAuth(@NotNull String login, @NotNull String password, @NotNull String accessToken) {
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

		@NotNull
		public String getAccessToken() {
			return myAccessToken;
		}

		private void setAccessToken(@NotNull String myAccessToken) {
			this.myAccessToken = myAccessToken;
		}
	}


}
