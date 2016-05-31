/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package org.intellij.gitosc.api;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/api/GithubUser.java
 * @author JetBrains s.r.o.
 * @author Aleksey Pivovarov
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
