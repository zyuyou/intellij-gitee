package org.intellij.gitosc.util;

import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.ide.passwordSafe.PasswordSafeException;
import com.intellij.ide.passwordSafe.config.PasswordSafeSettings;
import com.intellij.ide.passwordSafe.impl.PasswordSafeImpl;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import org.intellij.gitosc.api.GitoscApiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("MethodMayBeStatic")
@State(name = "GitoscSettings", storages = @Storage("gitosc_settings.xml"))
public class GitoscSettings implements PersistentStateComponent<GitoscSettings.State> {
	private static final Logger LOG = GitoscUtil.LOG;
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
		@NotNull public String HOST = GitoscApiUtil.DEFAULT_GITOSC_HOST;
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
		myState.HOST = StringUtil.notNullize(host, GitoscApiUtil.DEFAULT_GITOSC_HOST);
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
		final PasswordSafeImpl passwordSafe = (PasswordSafeImpl) PasswordSafe.getInstance();
		return passwordSafe.getSettings().getProviderType() == PasswordSafeSettings.ProviderType.MASTER_PASSWORD;
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
		String password;
		try {
			password = PasswordSafe.getInstance().getPassword(null, GitoscSettings.class, GITOSC_SETTINGS_PASSWORD_KEY);
		}
		catch (PasswordSafeException e) {
			LOG.info("Couldn't get password for key [" + GITOSC_SETTINGS_PASSWORD_KEY + "]", e);
			password = "";
		}

		return StringUtil.notNullize(password);
	}

	private void setPassword(@NotNull String password, boolean rememberPassword) {
		try {
			if (rememberPassword) {
				PasswordSafe.getInstance().storePassword(null, GitoscSettings.class, GITOSC_SETTINGS_PASSWORD_KEY, password);
			}
			else {
				final PasswordSafeImpl passwordSafe = (PasswordSafeImpl)PasswordSafe.getInstance();
				if (passwordSafe.getSettings().getProviderType() != PasswordSafeSettings.ProviderType.DO_NOT_STORE) {
					passwordSafe.getMemoryProvider().storePassword(null, GitoscSettings.class, GITOSC_SETTINGS_PASSWORD_KEY, password);
				}
			}
		}
		catch (PasswordSafeException e) {
			LOG.info("Couldn't set password for key [" + GITOSC_SETTINGS_PASSWORD_KEY + "]", e);
		}
	}

	@NotNull
	private String getAccessToken() {
		String accessToken;
		try {
			accessToken = PasswordSafe.getInstance().getPassword(null, GitoscSettings.class, GITOSC_SETTINGS_ACCESS_TOKEN_KEY);
		}
		catch (PasswordSafeException e) {
			LOG.info("Couldn't get access token for key [" + GITOSC_SETTINGS_ACCESS_TOKEN_KEY + "]", e);
			accessToken = "";
		}

		return StringUtil.notNullize(accessToken);
	}

	private void setAccessToken(@NotNull String accessToken, boolean rememberPassword) {
		try {
			if (rememberPassword) {
				PasswordSafe.getInstance().storePassword(null, GitoscSettings.class, GITOSC_SETTINGS_ACCESS_TOKEN_KEY, accessToken);
			}
			else {
				final PasswordSafeImpl passwordSafe = (PasswordSafeImpl)PasswordSafe.getInstance();
				if (passwordSafe.getSettings().getProviderType() != PasswordSafeSettings.ProviderType.DO_NOT_STORE) {
					passwordSafe.getMemoryProvider().storePassword(null, GitoscSettings.class, GITOSC_SETTINGS_ACCESS_TOKEN_KEY, accessToken);
				}
			}
		}
		catch (PasswordSafeException e) {
			LOG.info("Couldn't set access token for key [" + GITOSC_SETTINGS_ACCESS_TOKEN_KEY + "]", e);
		}
	}

	private static boolean isValidGitAuth(@NotNull GitoscAuthData auth) {
		switch (auth.getAuthType()) {
			case SESSION:
				return true;
			case BASIC:
//				assert auth.getBasicAuth() != null;
//				return auth.getBasicAuth().getCode() == null;
			case TOKEN:
//				return true;
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
			case BASIC:
				//noinspection ConstantConditions
				return GitoscAuthData.createBasicAuth(getHost(), getLogin(), getPassword());
			case TOKEN:
				return GitoscAuthData.createTokenAuth(getHost(), getPassword());
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
				assert auth.getSessionAuth() != null;
				setLogin(auth.getSessionAuth().getLogin());
				setPassword(auth.getSessionAuth().getPassword(), rememberPassword);
				setAccessToken(auth.getSessionAuth().getAccessToken(), rememberPassword);
				break;
			case BASIC:
				assert auth.getBasicAuth() != null;
				setLogin(auth.getBasicAuth().getLogin());
				setPassword(auth.getBasicAuth().getPassword(), rememberPassword);
				break;
			case TOKEN:
				assert auth.getTokenAuth() != null;
				setLogin(null);
				setPassword(auth.getTokenAuth().getToken(), rememberPassword);
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
