/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package org.intellij.gitosc.extensions;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.AuthData;
import git4idea.remote.GitHttpAuthDataProvider;
import org.intellij.gitosc.util.GitoscAuthData;
import org.intellij.gitosc.util.GitoscSettings;
import org.intellij.gitosc.util.GitoscUrlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/extensions/GithubHttpAuthDataProvider.java
 * @author JetBrains s.r.o.
 * @author Kirill Likhodedov
 */
public class GitoscHttpAuthDataProvider implements GitHttpAuthDataProvider {
	@Nullable
	@Override
	public AuthData getAuthData(@NotNull String url) {
		if(!GitoscUrlUtil.isGitoscUrl(url)){
			return null;
		}

		GitoscSettings settings = GitoscSettings.getInstance();
		if(!settings.isValidGitAuth()){
			return null;
		}

		String host1 = GitoscUrlUtil.getHostFromUrl(settings.getHost());
		String host2 = GitoscUrlUtil.getHostFromUrl(url);
		if(!host1.equalsIgnoreCase(host2)){
			return null;
		}

		GitoscAuthData auth = settings.getAuthData();
		switch (auth.getAuthType()){
			case SESSION:
				GitoscAuthData.SessionAuth sessionAuth = auth.getSessionAuth();
				assert sessionAuth != null;
				if(StringUtil.isEmptyOrSpaces(sessionAuth.getLogin()) || StringUtil.isEmptyOrSpaces(sessionAuth.getPassword())){
					return null;
				}
				return new AuthData(sessionAuth.getLogin(), sessionAuth.getPassword());
			case BASIC:
			case TOKEN:
			default:
				return null;
		}
	}

	@Override
	public void forgetPassword(@NotNull String url) {
		GitoscSettings.getInstance().setValidGitAuth(false);
	}
}
