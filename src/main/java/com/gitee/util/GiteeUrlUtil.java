/*
 * Copyright 2016-2018 码云 - Gitee
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
package com.gitee.util;

import com.gitee.GiteeConstants;
import com.gitee.api.GiteeFullPath;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Yuyou Chow
 *
 * Base on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/util/GithubUrlUtil.java
 * @author JetBrains s.r.o.
 * @author Aleksey Pivovarov
 */
public class GiteeUrlUtil {
	@NotNull
	public static String removeProtocolPrefix(String url) {
		int index = url.indexOf('@');
		if (index != -1) {
			return url.substring(index + 1).replace(':', '/');
		}
		index = url.indexOf("://");
		if (index != -1) {
			return url.substring(index + 3);
		}
		return url;
	}

	@NotNull
	public static String removeTrailingSlash(@NotNull String s) {
		if (s.endsWith("/")) {
			return s.substring(0, s.length() - 1);
		}
		return s;
	}

	/**
	 * Will only work correctly after {@link #removeProtocolPrefix(String)}
	 */
	@NotNull
	public static String removePort(@NotNull String url) {
		int index = url.indexOf(':');
		if (index == -1) return url;
		int slashIndex = url.indexOf('/');
		if (slashIndex != -1 && slashIndex < index) return url;

		String beforePort = url.substring(0, index);
		if (slashIndex == -1) {
			return beforePort;
		}
		else {
			return beforePort + url.substring(slashIndex);
		}
	}

	/**
	 * E.g.: https://gitee.com/api/v3
	 */
	@NotNull
	public static String getApiUrl() {
		return "";
//		return getApiUrl(GiteeSettings.getInstance().getHost());
	}

	@NotNull
	public static String getApiUrl(@NotNull String urlFromSettings) {
		return getApiProtocolFromUrl(urlFromSettings) + getApiUrlWithoutProtocol(urlFromSettings);
	}

	@NotNull
	public static String getApiProtocol() {
		return "";
//		return getApiProtocolFromUrl(GiteeSettings.getInstance().getHost());
	}

	@NotNull
	public static String getApiProtocolFromUrl(@NotNull String urlFromSettings) {
		if (StringUtil.startsWithIgnoreCase(urlFromSettings.trim(), "http://")){
			return "http://";
		}

		return "https://";
	}

	/**
	 * Returns the "host" part of Gitee URLs.
	 * E.g.: https://gitee.com
	 * Note: there is no trailing slash in the returned url.
	 */
	@NotNull
	public static String getGiteeHost() {
		return getApiProtocol() + getGitHostWithoutProtocol();
	}

	/**
	 * E.g.: https://gitee.com/suffix/ -> gitee.com
	 *       gitee.com:8080/ -> gitee.com
	 */
	@NotNull
	public static String getHostFromUrl(@NotNull String url) {
		String path = removeProtocolPrefix(url).replace(':', '/');
		int index = path.indexOf('/');
		if (index == -1) {
			return path;
		}
		else {
			return path.substring(0, index);
		}
	}


	/**
	 * E.g.: gitee.com
	 */
	@NotNull
	public static String getGitHostWithoutProtocol() {
		return "";
//		return removeTrailingSlash(removeProtocolPrefix(GiteeSettings.getInstance().getHost()));
	}

	@NotNull
	public static String getApiUrlWithoutProtocol(@NotNull String urlFromSettings) {
		String url = removeTrailingSlash(removeProtocolPrefix(urlFromSettings.toLowerCase()));

		final String API_SUFFIX = "/api/v3";

		if (url.equals(GiteeConstants.DEFAULT_GITEE_HOST)) {
			return url + API_SUFFIX;
		}
		else if (url.equals(GiteeConstants.DEFAULT_GITEE_HOST + API_SUFFIX)) {
			return url;
		}
		else{
			// have no custom Gitee url yet.
			return GiteeConstants.DEFAULT_GITEE_HOST + API_SUFFIX;
		}
	}

	/**
	 * 是否Gitee仓库地址
	 * */
	public static boolean isGiteeUrl(@NotNull String url) {
		return false;
//		return isGiteeUrl(url, GiteeSettings.getInstance().getHost());
	}

	public static boolean isGiteeUrl(@NotNull String url, @NotNull String host) {
		host = getHostFromUrl(host);
		url = removeProtocolPrefix(url);
		return StringUtil.startsWithIgnoreCase(url, host)
			&& !(url.length() > host.length() && ":/".indexOf(url.charAt(host.length())) == -1);
	}

	/**
	 * assumed isGiteeUrl(remoteUrl)
	 *
	 * git@gitee.com:user/repo.git -> user/repo
	 */
	@Nullable
	public static GiteeFullPath getUserAndRepositoryFromRemoteUrl(@NotNull String remoteUrl) {
		remoteUrl = removeProtocolPrefix(removeEndingDotGit(remoteUrl));
		int index1 = remoteUrl.lastIndexOf('/');
		if (index1 == -1) {
			return null;
		}
		String url = remoteUrl.substring(0, index1);
		int index2 = Math.max(url.lastIndexOf('/'), url.lastIndexOf(':'));
		if (index2 == -1) {
			return null;
		}
		final String username = remoteUrl.substring(index2 + 1, index1);
		final String reponame = remoteUrl.substring(index1 + 1);
		if (username.isEmpty() || reponame.isEmpty()) {
			return null;
		}
		return new GiteeFullPath(username, reponame);
	}

	@NotNull
	private static String removeEndingDotGit(@NotNull String url) {
		url = removeTrailingSlash(url);
		final String DOT_GIT = ".git";
		if (url.endsWith(DOT_GIT)) {
			return url.substring(0, url.length() - DOT_GIT.length());
		}
		return url;
	}

	/**
	 * assumed isGiteeUrl(remoteUrl)
	 *
	 * git@gitee.com:user/repo -> https://gitee.com/user/repo
	 */
	@Nullable
	public static String makeGiteeRepoUrlFromRemoteUrl(@NotNull String remoteUrl) {
		return makeGiteeRepoUrlFromRemoteUrl(remoteUrl, getGiteeHost());
	}

	@Nullable
	public static String makeGiteeRepoUrlFromRemoteUrl(@NotNull String remoteUrl, @NotNull String host) {
		GiteeFullPath repo = getUserAndRepositoryFromRemoteUrl(remoteUrl);
		if (repo == null) {
			return null;
		}
		return host + '/' + repo.getUser() + '/' + repo.getRepository();
	}

	@NotNull
	public static String getCloneUrl(@NotNull GiteeFullPath path) {
		return StringUtil.isEmptyOrSpaces(path.getFullName()) ?
			getCloneUrl(path.getUser(), path.getRepository()) : getCloneUrlFromFullName(path.getFullName());
	}

	@NotNull
	public static String getCloneUrl(@NotNull String user, @NotNull String repo) {
		if (GiteeSettings.getInstance().isCloneGitUsingSsh()) {
			return "git@" + getGitHostWithoutProtocol() + ":" + user + "/" + repo + ".git";
		}
		else {
			return getApiProtocol() + getGitHostWithoutProtocol() + "/" + user + "/" + repo + ".git";
		}
	}

	@NotNull
	private static String getCloneUrlFromFullName(@NotNull String fullName) {
		if (GiteeSettings.getInstance().isCloneGitUsingSsh()) {
			return "git@" + getGitHostWithoutProtocol() + ":" + fullName + ".git";
		}
		else {
			return getApiProtocol() + getGitHostWithoutProtocol() + "/" + fullName + ".git";
		}
	}
}
