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
package org.intellij.gitee.extensions;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.AuthData;
import git4idea.remote.GitHttpAuthDataProvider;
import org.intellij.gitee.util.GiteeAuthData;
import org.intellij.gitee.util.GiteeSettings;
import org.intellij.gitee.util.GiteeUrlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/extensions/GithubHttpAuthDataProvider.java
 * @author JetBrains s.r.o.
 * @author Kirill Likhodedov
 */
public class GiteeHttpAuthDataProvider implements GitHttpAuthDataProvider {
	@Nullable
	@Override
	public AuthData getAuthData(@NotNull String url) {
		if(!GiteeUrlUtil.isGitoscUrl(url)){
			return null;
		}

		GiteeSettings settings = GiteeSettings.getInstance();
		if(!settings.isValidGitAuth()){
			return null;
		}

		String host1 = GiteeUrlUtil.getHostFromUrl(settings.getHost());
		String host2 = GiteeUrlUtil.getHostFromUrl(url);
		if(!host1.equalsIgnoreCase(host2)){
			return null;
		}

		GiteeAuthData auth = settings.getAuthData();
		switch (auth.getAuthType()){
			case SESSION:
				GiteeAuthData.SessionAuth sessionAuth = auth.getSessionAuth();
				assert sessionAuth != null;
				if(StringUtil.isEmptyOrSpaces(sessionAuth.getLogin()) || StringUtil.isEmptyOrSpaces(sessionAuth.getPassword())){
					return null;
				}
				return new AuthData(sessionAuth.getLogin(), sessionAuth.getPassword());
			case TOKEN:
				GiteeAuthData.TokenAuth tokenAuth = auth.getTokenAuth();
				assert tokenAuth != null;
				if (StringUtil.isEmptyOrSpaces(tokenAuth.getToken())) {
					return null;
				}
				return new AuthData(tokenAuth.getToken(), "x-oauth-basic");
			case BASIC:
			default:
				return null;
		}
	}

	@Override
	public void forgetPassword(@NotNull String url) {
		GiteeSettings.getInstance().setValidGitAuth(false);
	}
}
