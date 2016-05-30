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

import org.intellij.gitosc.util.GitoscUrlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 *  https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/api/GithubRepo.java
 */
public class GitoscRepo {
	@NotNull private final String myName;
	@NotNull private final String myDesc;

	private final boolean myIsPublic;
	private final boolean myIsFork;

	@NotNull private final String myPath;
	@NotNull private final String myPathWithNamespace;

	@Nullable private final String myDefaultBranch;

	@NotNull private final GitoscUser myOwner;

	public GitoscRepo(@NotNull String myName,
	                  @NotNull String myDesc,
	                  boolean myIsPublic,
	                  boolean myIsFork,
	                  @NotNull String myPath,
	                  @NotNull String myPathWithNamespace,
	                  @Nullable String myDefaultBranch,
	                  @NotNull GitoscUser myOwner) {

		this.myName = myName;
		this.myDesc = myDesc;
		this.myIsPublic = myIsPublic;
		this.myIsFork = myIsFork;
		this.myPath = myPath;
		this.myPathWithNamespace = myPathWithNamespace;
		this.myDefaultBranch = myDefaultBranch;
		this.myOwner = myOwner;
	}

	@NotNull
	public String getName() {
		return myName;
	}

	@NotNull
	public String getDesc() {
		return myDesc;
	}

	public boolean isPublic() {
		return myIsPublic;
	}

	public boolean isFork() {
		return myIsFork;
	}

	@NotNull
	public String getPath() {
		return myPath;
	}

	@NotNull
	public String getPathWithNamespace() {
		return myPathWithNamespace;
	}

	@Nullable
	public String getDefaultBranch() {
		return myDefaultBranch;
	}

	@NotNull
	public GitoscUser getOwner() {
		return myOwner;
	}

	@NotNull
	public String getUserName(){
		return getOwner().getLogin();
	}

	@NotNull
	public GitoscFullPath getFullPath(){
		String[] paths = getPathWithNamespace().split("/");
		return new GitoscFullPath(paths[0], paths[1]);
	}

	@NotNull
	public String getHtmlUrl(){
		String[] paths = getPathWithNamespace().split("/");
		if(paths.length == 2){
			return GitoscUrlUtil.getApiProtocol() + GitoscUrlUtil.getGitHostWithoutProtocol() + "/" + paths[0] + "/" + paths[1] + ".git";
		}else{
			return GitoscUrlUtil.getApiProtocol() + GitoscUrlUtil.getGitHostWithoutProtocol() + "/" + getUserName() + "/" + paths[0] + ".git";
		}
	}
}
