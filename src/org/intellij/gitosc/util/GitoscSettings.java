/*
 * Copyright 2016 码云
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
package org.intellij.gitosc.util;

import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.util.text.StringUtil;
import org.intellij.gitosc.GitoscConstants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/util/GithubSettings.java
 * @author JetBrains s.r.o.
 * @author oleg
 */
@SuppressWarnings("MethodMayBeStatic")
@State(name = "GitoscSettings", storages = @Storage("gitosc_settings.xml"))
public class GitoscSettings implements PersistentStateComponent<GitoscSettings.State> {

	private static final String GITOSC_SETTINGS_PASSWORD_KEY = "GITOSC_SETTINGS_PASSWORD_KEY";
	private static final String GITOSC_SETTINGS_ACCESS_TOKEN_KEY = "GITOSC_SETTINGS_ACCESS_TOKEN_KEY";

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

	public static GitoscSettings getInstance() {
		return ServiceManager.getService(GitoscSettings.class);
	}

	public static class State {
		@NotNull public String HOST = GitoscConstants.DEFAULT_GITOSC_HOST;
		@Nullable public String LOGIN = null;
		@NotNull public GitoscAuthData.AuthType AUTH_TYPE = GitoscAuthData.AuthType.ANONYMOUS;

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
		myState.HOST = StringUtil.notNullize(host, GitoscConstants.DEFAULT_GITOSC_HOST);
	}

	@Nullable
	public String getLogin(){
		return myState.LOGIN;
	}

	private void setLogin(@Nullable String login) {
		myState.LOGIN = login;
	}

	@NotNull
	public GitoscAuthData.AuthType getAuthType() {
		return myState.AUTH_TYPE;
	}

	private void setAuthType(@NotNull GitoscAuthData.AuthType authType) {
		myState.AUTH_TYPE = authType;
	}

	public boolean isAuthConfigured() {
		return !myState.AUTH_TYPE.equals(GitoscAuthData.AuthType.ANONYMOUS);
	}

	public boolean isSavePassword() {
		return myState.SAVE_PASSWORD;
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
		return StringUtil.notNullize(PasswordSafe.getInstance().getPassword(GitoscSettings.class, GITOSC_SETTINGS_PASSWORD_KEY));
	}

	private void setPassword(@NotNull String password, boolean rememberPassword) {
		if (!rememberPassword) return;
		PasswordSafe.getInstance().setPassword(GitoscSettings.class, GITOSC_SETTINGS_PASSWORD_KEY, password);
	}

	@NotNull
	private String getAccessToken() {
		return StringUtil.notNullize(PasswordSafe.getInstance().getPassword(GitoscSettings.class, GITOSC_SETTINGS_ACCESS_TOKEN_KEY));
	}

	private void setAccessToken(@NotNull String accessToken, boolean rememberPassword) {
		if (!rememberPassword) return;
		PasswordSafe.getInstance().setPassword(GitoscSettings.class, GITOSC_SETTINGS_ACCESS_TOKEN_KEY, accessToken);
	}

	private static boolean isValidGitAuth(@NotNull GitoscAuthData auth) {
		switch (auth.getAuthType()) {
			case SESSION:
				return true;
			case ANONYMOUS:
				return false;
			default:
				throw new IllegalStateException("GitoscSettings: setAuthData - wrong AuthType: " + auth.getAuthType());
		}
	}

	@NotNull
	public GitoscAuthData getAuthData() {
		switch (getAuthType()) {
			case SESSION:
				return GitoscAuthData.createSessionAuth(getHost(), getLogin(), getPassword(), getAccessToken());
			case ANONYMOUS:
				return GitoscAuthData.createAnonymous();
			default:
				throw new IllegalStateException("GitoscSettings: getAuthData - wrong AuthType: " + getAuthType());
		}
	}

	public void setAuthData(@NotNull GitoscAuthData auth, boolean rememberPassword) {
		setValidGitAuth(isValidGitAuth(auth));

		setAuthType(auth.getAuthType());
		setHost(auth.getHost());

		switch (auth.getAuthType()) {
			case SESSION:
				GitoscAuthData.SessionAuth sessionAuth = auth.getSessionAuth();
				assert sessionAuth != null;
				setLogin(sessionAuth.getLogin());
				setPassword(sessionAuth.getPassword(), rememberPassword);
				setAccessToken(sessionAuth.getAccessToken() == null ? "":sessionAuth.getAccessToken(), rememberPassword);
				break;
			case ANONYMOUS:
				setLogin(null);
				setPassword("", rememberPassword);
				break;
			default:
				throw new IllegalStateException("GitoscSettings: setAuthData - wrong AuthType: " + auth.getAuthType());
		}
	}
}