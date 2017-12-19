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
package org.intellij.gitosc.util;

import com.intellij.openapi.util.text.StringUtil;
import org.intellij.gitosc.GitoscConstants;
import org.intellij.gitosc.api.GitoscApiUtil;
import org.intellij.gitosc.api.GitoscFullPath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Yuyou Chow
 *
 * Base on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/util/GithubUrlUtil.java
 * @author JetBrains s.r.o.
 * @author Aleksey Pivovarov
 */
public class GitoscUrlUtil {
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
	 * E.g.: https://gitee.com/api/v3
	 */
	@NotNull
	public static String getApiUrl() {
		return getApiUrl(GitoscSettings.getInstance().getHost());
	}

	@NotNull
	public static String getApiUrl(@NotNull String urlFromSettings) {
		return getApiProtocolFromUrl(urlFromSettings) + getApiUrlWithoutProtocol(urlFromSettings);
	}

	@NotNull
	public static String getApiUrl(@NotNull GitoscAuthData auth) {
		return getApiProtocolFromUrl(auth.getHost()) + getApiUrlWithoutProtocol(auth);
	}

	@NotNull
	public static String getApiProtocol() {
		return getApiProtocolFromUrl(GitoscSettings.getInstance().getHost());
	}

	@NotNull
	public static String getApiProtocolFromUrl(@NotNull String urlFromSettings) {
		if (StringUtil.startsWithIgnoreCase(urlFromSettings.trim(), "http://")){
			return "http://";
		}

		return "https://";
	}

	/**
	 * Returns the "host" part of Gitosc URLs.
	 * E.g.: https://gitee.com
	 * Note: there is no trailing slash in the returned url.
	 */
	@NotNull
	public static String getGitoscHost() {
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
		return removeTrailingSlash(removeProtocolPrefix(GitoscSettings.getInstance().getHost()));
	}

	@NotNull
	public static String getApiUrlWithoutProtocol(@NotNull String urlFromSettings) {
		String url = removeTrailingSlash(removeProtocolPrefix(urlFromSettings.toLowerCase()));

		final String API_SUFFIX = "/api/v3";

		if (url.equals(GitoscConstants.DEFAULT_GITOSC_HOST)) {
			return url + API_SUFFIX;
		}
		else if (url.equals(GitoscConstants.DEFAULT_GITOSC_HOST + API_SUFFIX)) {
			return url;
		}
		else{
			// have no custom GitOSC url yet.
			return GitoscConstants.DEFAULT_GITOSC_HOST + API_SUFFIX;
		}
	}

	@NotNull
	public static String getApiUrlWithoutProtocol(@NotNull GitoscAuthData auth) {
		String url = removeTrailingSlash(removeProtocolPrefix(auth.getHost().toLowerCase()));

		final String API_SUFFIX = auth.getTokenAuth() == null ? "" : "/api/v5";

		if (url.equals(GitoscConstants.DEFAULT_GITOSC_HOST)) {
			return url + API_SUFFIX;
		}
		else if (url.equals(GitoscConstants.DEFAULT_GITOSC_HOST + API_SUFFIX)) {
			return url;
		}
		else{
			// have no custom GitOSC url yet.
			return GitoscConstants.DEFAULT_GITOSC_HOST + API_SUFFIX;
		}
	}

	/**
	 * 是否GitOSC仓库地址
	 * */
	public static boolean isGitoscUrl(@NotNull String url) {
		return isGitoscUrl(url, GitoscSettings.getInstance().getHost());
	}

	public static boolean isGitoscUrl(@NotNull String url, @NotNull String host) {
		host = getHostFromUrl(host);
		url = removeProtocolPrefix(url);
		return StringUtil.startsWithIgnoreCase(url, host)
			&& !(url.length() > host.length() && ":/".indexOf(url.charAt(host.length())) == -1);
	}

	/**
	 * assumed isGitoscUrl(remoteUrl)
	 *
	 * git@gitee.com:user/repo.git -> user/repo
	 */
	@Nullable
	public static GitoscFullPath getUserAndRepositoryFromRemoteUrl(@NotNull String remoteUrl) {
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
		return new GitoscFullPath(username, reponame);
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
	 * assumed isGitoscUrl(remoteUrl)
	 *
	 * git@gitee.com:user/repo -> https://gitee.com/user/repo
	 */
	@Nullable
	public static String makeGitoscRepoUrlFromRemoteUrl(@NotNull String remoteUrl) {
		return makeGitoscRepoUrlFromRemoteUrl(remoteUrl, getGitoscHost());
	}

	@Nullable
	public static String makeGitoscRepoUrlFromRemoteUrl(@NotNull String remoteUrl, @NotNull String host) {
		GitoscFullPath repo = getUserAndRepositoryFromRemoteUrl(remoteUrl);
		if (repo == null) {
			return null;
		}
		return host + '/' + repo.getUser() + '/' + repo.getRepository();
	}

	@NotNull
	public static String getCloneUrl(@NotNull GitoscFullPath path) {
		return getCloneUrl(path.getUser(), path.getRepository());
	}

	@NotNull
	public static String getCloneUrl(@NotNull String user, @NotNull String repo) {
		if (GitoscSettings.getInstance().isCloneGitUsingSsh()) {
			return "git@" + getGitHostWithoutProtocol() + ":" + user + "/" + repo + ".git";
		}
		else {
			return getApiProtocol() + getGitHostWithoutProtocol() + "/" + user + "/" + repo + ".git";
		}
	}
}
