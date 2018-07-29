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
package org.intellij.gitee.util;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.util.text.StringUtil;
import org.intellij.gitee.GiteeConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.intellij.gitee.GiteeConstants.LOG;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/util/GithubSettings.java
 * @author JetBrains s.r.o.
 * @author oleg
 */
@SuppressWarnings("MethodMayBeStatic")
@State(name = "GiteeSettings", storages = @Storage("gitee_settings.xml"))
public class GiteeSettings implements PersistentStateComponent<GiteeSettings.State> {

	private static final String GITEE_SETTINGS_PASSWORD_KEY = "GITEE_SETTINGS_PASSWORD_KEY";
	private static final String GITEE_SETTINGS_ACCESS_TOKEN_KEY = "GITEE_SETTINGS_ACCESS_TOKEN_KEY";
	private static final String GITEE_SETTINGS_REFRESH_TOKEN_KEY = "GITEE_SETTINGS_REFRESH_TOKEN_KEY";

	private State myState = new State();

	@Nullable
	@Override
	public State getState() {
		return myState;
	}

	@Override
	public void loadState(State state) {
		myState = state;
	}

	public static GiteeSettings getInstance() {
		return ServiceManager.getService(GiteeSettings.class);
	}

	public static class State {
		@Nullable public String LOGIN = null;
		@NotNull public String HOST = GiteeConstants.DEFAULT_GITEE_HOST;
		@NotNull public GiteeAuthData.AuthType AUTH_TYPE = GiteeAuthData.AuthType.ANONYMOUS;

		public boolean ANONYMOUS_GIST = false;
		public boolean OPEN_IN_BROWSER_GIST = true;
		public boolean PRIVATE_GIST = true;

		public boolean SAVE_PASSWORD = true;
		public int CONNECTION_TIMEOUT = 5000;
		public boolean VALID_GIT_AUTH = true;

		public boolean CLONE_GIT_USING_SSH = false;
	}

	@NotNull
	public String getHost(){
		return myState.HOST;
	}

	private void setHost(@NotNull String host) {
		myState.HOST = StringUtil.notNullize(host, GiteeConstants.DEFAULT_GITEE_HOST);
	}

	@Nullable
	public String getLogin(){
		return myState.LOGIN;
	}

	private void setLogin(@Nullable String login) {
		myState.LOGIN = login;
	}

	@NotNull
	public GiteeAuthData.AuthType getAuthType() {
		return myState.AUTH_TYPE;
	}

	private void setAuthType(@NotNull GiteeAuthData.AuthType authType) {
		myState.AUTH_TYPE = authType;
	}

	public boolean isAnonymousGist() {
		return myState.ANONYMOUS_GIST;
	}

	public boolean isOpenInBrowserGist() {
		return myState.OPEN_IN_BROWSER_GIST;
	}

	public boolean isPrivateGist() {
		return myState.PRIVATE_GIST;
	}

	public boolean isAuthConfigured() {
		return !myState.AUTH_TYPE.equals(GiteeAuthData.AuthType.ANONYMOUS);
	}

	public boolean isSavePassword() {
		return myState.SAVE_PASSWORD;
	}

	public void setAnonymousGist(final boolean anonymousGist) {
		myState.ANONYMOUS_GIST = anonymousGist;
	}

	public void setPrivateGist(final boolean privateGist) {
		myState.PRIVATE_GIST = privateGist;
	}

	public void setOpenInBrowserGist(final boolean openInBrowserGist) {
		myState.OPEN_IN_BROWSER_GIST = openInBrowserGist;
	}

	public void setSavePassword(final boolean savePassword) {
		myState.SAVE_PASSWORD = savePassword;
	}

	public boolean isSavePasswordMakesSense() {
		return !PasswordSafe.getInstance().isMemoryOnly();
	}

	public boolean isValidGitAuth() {
		return myState.VALID_GIT_AUTH;
	}

	public void setValidGitAuth(final boolean validGitAuth) {
		myState.VALID_GIT_AUTH = validGitAuth;
	}

	public boolean isCloneGitUsingSsh() {
		return myState.CLONE_GIT_USING_SSH;
	}

	public void setCloneGitUsingSsh(boolean value) {
		myState.CLONE_GIT_USING_SSH = value;
	}

	public int getConnectionTimeout() {
		return myState.CONNECTION_TIMEOUT;
	}

	public void setConnectionTimeout(int timeout) {
		myState.CONNECTION_TIMEOUT = timeout;
	}

	@NotNull
	private String getPassword() {
		return StringUtil.notNullize(PasswordSafe.getInstance().getPassword(createCredentialAttributes(GITEE_SETTINGS_PASSWORD_KEY)));
	}

	private void setPassword(@NotNull String password, boolean rememberPassword) {
		if (!rememberPassword) return;
		PasswordSafe.getInstance().setPassword(createCredentialAttributes(GITEE_SETTINGS_PASSWORD_KEY), password);
	}

	@NotNull
	private String getAccessToken() {
		return StringUtil.notNullize(PasswordSafe.getInstance().getPassword(createCredentialAttributes(GITEE_SETTINGS_ACCESS_TOKEN_KEY)));
	}

	private void setAccessToken(@NotNull String accessToken, boolean rememberPassword) {
		if (!rememberPassword) return;
		PasswordSafe.getInstance().setPassword(createCredentialAttributes(GITEE_SETTINGS_ACCESS_TOKEN_KEY), accessToken);
	}

	@NotNull
	private String getRefreshToken() {
		return StringUtil.notNullize(PasswordSafe.getInstance().getPassword(createCredentialAttributes(GITEE_SETTINGS_REFRESH_TOKEN_KEY)));
	}

	private void setRefreshToken(@NotNull String refreshToken, boolean rememberPassword) {
		if (!rememberPassword) return;
		PasswordSafe.getInstance().setPassword(createCredentialAttributes(GITEE_SETTINGS_REFRESH_TOKEN_KEY), refreshToken);
	}

	private static CredentialAttributes createCredentialAttributes(@NotNull String userName) {
		return new CredentialAttributes(GiteeSettings.class.getName() + "@" + userName, userName);
	}

	private static boolean isValidGitAuth(@NotNull GiteeAuthData auth) {
		switch (auth.getAuthType()) {
			case SESSION:
				return true;
			case TOKEN:
				return true;
			case ANONYMOUS:
				return false;
			default:
				throw new IllegalStateException("GiteeSettings: setAuthData - wrong AuthType: " + auth.getAuthType());
		}
	}

	@NotNull
	public GiteeAuthData getAuthData() {
		switch (getAuthType()) {
			case SESSION:
				return GiteeAuthData.createSessionAuth(getHost(), getLogin(), getPassword(), getAccessToken());
			case TOKEN:
				return GiteeAuthData.createTokenAuth(getHost(), getAccessToken(), getRefreshToken());
			case ANONYMOUS:
				return GiteeAuthData.createAnonymous();
			default:
				throw new IllegalStateException("GiteeSettings: getAuthData - wrong AuthType: " + getAuthType());
		}
	}

	public void setAuthData(@NotNull GiteeAuthData auth, boolean rememberPassword) {
		setValidGitAuth(isValidGitAuth(auth));

		setAuthType(auth.getAuthType());
		setHost(auth.getHost());

		switch (auth.getAuthType()) {
			case SESSION:
				GiteeAuthData.SessionAuth sessionAuth = auth.getSessionAuth();
				assert sessionAuth != null;
				setLogin(sessionAuth.getLogin());
				setPassword(sessionAuth.getPassword(), rememberPassword);
				setAccessToken(sessionAuth.getAccessToken() == null ? "" : sessionAuth.getAccessToken(), rememberPassword);
				break;
			case TOKEN:
				GiteeAuthData.TokenAuth tokenAuth = auth.getTokenAuth();
				assert tokenAuth != null;
				setLogin(null);
//				setPassword(tokenAuth.getToken(), rememberPassword);
				setAccessToken(tokenAuth.getToken(), rememberPassword);
				setRefreshToken(tokenAuth.getRefreshToken(), rememberPassword);
				break;
			case ANONYMOUS:
				setLogin(null);
				setPassword("", rememberPassword);
				break;
			default:
				LOG.error("GiteeSettings: setAuthData - wrong AuthType: " + auth.getAuthType());
				throw new IllegalStateException("GiteeSettings: setAuthData - wrong AuthType: " + auth.getAuthType());
		}
	}
}