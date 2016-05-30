/*
 * Copyright 2013-2016 Yuyou Chow
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

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *  https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/api/GithubFullPath.java
 */
public class GitoscFullPath {
	@NotNull private final String myUserName;
	@NotNull private final String myRepositoryName;

	public GitoscFullPath(@NotNull String myUserName, @NotNull String myRepositoryName) {
		this.myUserName = myUserName;
		this.myRepositoryName = myRepositoryName;
	}

	@NotNull
	public String getUser() {
		return myUserName;
	}

	@NotNull
	public String getRepository() {
		return myRepositoryName;
	}

	@NotNull
	public String getFullName(){
		return myUserName + "/" + myRepositoryName;
	}

	@Override
	public String toString() {
		return "'" + getFullName() + "'";
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		GitoscFullPath that = (GitoscFullPath)o;

		if (!StringUtil.equalsIgnoreCase(myRepositoryName, that.myRepositoryName)) return false;
		if (!StringUtil.equalsIgnoreCase(myUserName, that.myUserName)) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = myUserName.hashCode();
		result = 31 * result + myRepositoryName.hashCode();
		return result;
	}

}
