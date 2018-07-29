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

import com.intellij.openapi.util.ThrowableComputable;
import org.jetbrains.annotations.NotNull;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/util/GithubAuthDataHolder.java
 * @author JetBrains s.r.o.
 */
public class GiteeAuthDataHolder {
	@NotNull private GiteeAuthData myAuthData;

	public GiteeAuthDataHolder(@NotNull GiteeAuthData auth){
		myAuthData = auth;
	}

	@NotNull
	public synchronized GiteeAuthData getAuthData(){
		return myAuthData;
	}

	public synchronized <T extends Throwable> void runTransaction(@NotNull GiteeAuthData expected,
	                                                              @NotNull ThrowableComputable<GiteeAuthData, T> task) throws T {
		if(expected != myAuthData){
			return;
		}

		myAuthData = task.compute();
	}

	public static GiteeAuthDataHolder createFromSettings(){
		return new GiteeAuthDataHolder(GiteeSettings.getInstance().getAuthData());
	}

	public static GiteeAuthDataHolder createForLogin() {
		return new GiteeAuthDataHolder(GiteeAuthData.createAnonymous());
	}
}
