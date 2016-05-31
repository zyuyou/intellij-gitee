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

import com.intellij.openapi.util.ThrowableComputable;
import org.jetbrains.annotations.NotNull;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/util/GithubAuthDataHolder.java
 * @author JetBrains s.r.o.
 */
public class GitoscAuthDataHolder {
	@NotNull private GitoscAuthData myAuthData;

	public GitoscAuthDataHolder(@NotNull GitoscAuthData auth){
		myAuthData = auth;
	}

	@NotNull
	public synchronized GitoscAuthData getAuthData(){
		return myAuthData;
	}

	public synchronized <T extends Throwable> void runTransaction(@NotNull GitoscAuthData expected,
	                                                              @NotNull ThrowableComputable<GitoscAuthData, T> task) throws T {
		if(expected != myAuthData){
			return;
		}

		myAuthData = task.compute();
	}

	public static GitoscAuthDataHolder createFromSettings(){
		return new GitoscAuthDataHolder(GitoscSettings.getInstance().getAuthData());
	}
}
