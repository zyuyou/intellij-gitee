/*
 *  Copyright 2016-2019 码云 - Gitee
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.gitee.util;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.ThreeState;
import org.jetbrains.annotations.NotNull;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/util/GithubSettings.java
 * @author JetBrains s.r.o.
 * @author oleg
 */
@State(name = "GiteeSettings", storages = @Storage("gitee.xml"))
public class GiteeSettings implements PersistentStateComponent<GiteeSettings.State> {
	private State myState = new State();

	@Override
	public State getState() {
		return myState;
	}

	@Override
	public void loadState(@NotNull State state) {
		myState = state;
	}

	public static class State {
		public boolean OPEN_IN_BROWSER_GIST = true;
		public boolean COPY_URL_GIST = false;

		// "Secret" in UI, "Public" in API. "Private" here to preserve user settings after refactoring
		public boolean PRIVATE_GIST = true;

		public int CONNECTION_TIMEOUT = 5000;

		public ThreeState CREATE_PULL_REQUEST_CREATE_REMOTE = ThreeState.UNSURE;
		public boolean CLONE_GIT_USING_SSH = false;
	}

	public static GiteeSettings getInstance() {
		return ServiceManager.getService(GiteeSettings.class);
	}

	public boolean isOpenInBrowserGist() {
		return myState.OPEN_IN_BROWSER_GIST;
	}

	public void setOpenInBrowserGist(final boolean openInBrowserGist) {
		myState.OPEN_IN_BROWSER_GIST = openInBrowserGist;
	}

	public boolean isPrivateGist() {
		return myState.PRIVATE_GIST;
	}

	public void setPrivateGist(final boolean privateGist) {
		myState.PRIVATE_GIST = privateGist;
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

	public boolean isCopyURLGist() {
		return myState.COPY_URL_GIST;
	}

	public void setCopyURLGist(boolean copyLink) {
		myState.COPY_URL_GIST = copyLink;
	}

	@NotNull
	public ThreeState getCreatePullRequestCreateRemote() {
		return myState.CREATE_PULL_REQUEST_CREATE_REMOTE;
	}

	public void setCreatePullRequestCreateRemote(@NotNull ThreeState value) {
		myState.CREATE_PULL_REQUEST_CREATE_REMOTE = value;
	}
}