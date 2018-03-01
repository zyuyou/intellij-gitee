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
package org.intellij.gitosc.api;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/api/GithubFullPath.java
 * @author JetBrains s.r.o.
 * @author Aleksey Pivovarov
 */
public class GitoscFullPath {
	@NotNull private final String myUserName;
	@NotNull private final String myRepositoryName;
	@NotNull private final String myFullName;

	public GitoscFullPath(@NotNull String myUserName, @NotNull String myRepositoryName) {
		this.myUserName = myUserName;
		this.myRepositoryName = myRepositoryName;
		this.myFullName = "";
	}

	public GitoscFullPath(@NotNull String myUserName, @NotNull String myRepositoryName, @NotNull String myFullName) {
		this.myUserName = myUserName;
		this.myRepositoryName = myRepositoryName;
		this.myFullName = myFullName;
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
		return StringUtil.isEmptyOrSpaces(myFullName) ? myUserName + "/" + myRepositoryName : myFullName;
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
