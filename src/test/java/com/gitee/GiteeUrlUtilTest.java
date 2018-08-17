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

package com.gitee;

import com.gitee.api.GiteeFullPath;
import com.gitee.util.GiteeUrlUtil;
import com.intellij.openapi.util.Pair;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.util.containers.Convertor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GiteeUrlUtilTest extends UsefulTestCase {
	private static class TestCase<T> {
		@NotNull
		final public List<Pair<String, T>> tests = new ArrayList<Pair<String, T>>();

		public void add(@NotNull String in, @Nullable T out) {
			tests.add(Pair.create(in, out));
		}
	}

	private static <T> void runTestCase(@NotNull TestCase<T> tests, @NotNull Convertor<String, T> func) {
		for (Pair<String, T> test : tests.tests) {
			assertEquals(test.getFirst(), test.getSecond(), func.convert(test.getFirst()));
		}
	}

	// just for comment - passed
	public void testRemoveTrailingSlash() throws Throwable {
		TestCase<String> tests = new TestCase<String>();

		tests.add("http://git.oschina.net/", "http://git.oschina.net");
		tests.add("http://git.oschina.net", "http://git.oschina.net");

		tests.add("http://git.oschina.net/user/repo/", "http://git.oschina.net/user/repo");
		tests.add("http://git.oschina.net/user/repo", "http://git.oschina.net/user/repo");

		runTestCase(tests, new Convertor<String, String>() {
			@Override
			public String convert(String in) {
				return GiteeUrlUtil.removeTrailingSlash(in);
			}
		});
	}

	// just for comment - passed
	public void testRemoveProtocolPrefix() throws Throwable {
		TestCase<String> tests = new TestCase<String>();

		tests.add("git.oschina.net/user/repo/", "git.oschina.net/user/repo/");
		tests.add("git.oschina.net/api/v3/user/repo/", "git.oschina.net/api/v3/user/repo/");

		tests.add("http://git.oschina.net/user/repo/", "git.oschina.net/user/repo/");
		tests.add("https://git.oschina.net/user/repo/", "git.oschina.net/user/repo/");
		tests.add("git://git.oschina.net/user/repo/", "git.oschina.net/user/repo/");
		tests.add("git@git.oschina.net:user/repo/", "git.oschina.net/user/repo/");

		tests.add("git@git.oschina.net:username/repo/", "git.oschina.net/username/repo/");
		tests.add("https://username:password@git.oschina.net/user/repo/", "git.oschina.net/user/repo/");
		tests.add("https://username@git.oschina.net/user/repo/", "git.oschina.net/user/repo/");
		tests.add("https://git.oschina.net:2233/user/repo/", "git.oschina.net:2233/user/repo/");

		tests.add("HTTP://git.OSCHINA.net/user/repo/", "git.OSCHINA.net/user/repo/");
		tests.add("HttP://git.oschina.net/user/repo/", "git.oschina.net/user/repo/");

		runTestCase(tests, new Convertor<String, String>() {
			@Override
			public String convert(String in) {
				return GiteeUrlUtil.removeProtocolPrefix(in);
			}
		});
	}

	// just for comment - passed
	public void testIsGiteeUrl1() throws Throwable {
		TestCase<Boolean> tests = new TestCase<Boolean>();

		tests.add("http://git.oschina.net/user/repo", true);
		tests.add("https://git.oschina.net/user/repo", true);
		tests.add("git://git.oschina.net/user/repo", true);
		tests.add("git@git.oschina.net:user/repo", true);

		tests.add("https://git.oschina.net/", true);
		tests.add("git.oschina.net", true);

		tests.add("https://user@git.oschina.net/user/repo", true);
		tests.add("https://user:password@git.oschina.net/user/repo", true);
		tests.add("git@git.oschina.net:user/repo", true);

		tests.add("https://git.oschina.net:2233/", true);

		tests.add("HTTPS://git.oschina.net:2233/", true);

		tests.add("google.com", false);
		tests.add("git.oschina.net.site.ua", false);
		tests.add("sf@hskfh../.#fwenj 32#$", false);
		tests.add("api.git.oschina.net", false);
		tests.add("site.com//git.oschina.net", false);

		runTestCase(tests, new Convertor<String, Boolean>() {
			@Override
			public Boolean convert(String in) {
				return GiteeUrlUtil.isGiteeUrl(in, "https://git.oschina.net/");
			}
		});

		runTestCase(tests, new Convertor<String, Boolean>() {
			@Override
			public Boolean convert(String in) {
				return GiteeUrlUtil.isGiteeUrl(in, "http://git.OSCHINA.net");
			}
		});
	}

	// just for comment - passed
	public void testGetUserAndRepositoryFromRemoteUrl() throws Throwable {
		TestCase<GiteeFullPath> tests = new TestCase<GiteeFullPath>();

		tests.add("http://git.oschina.net/username/reponame/", new GiteeFullPath("username", "reponame"));
		tests.add("https://git.oschina.net/username/reponame/", new GiteeFullPath("username", "reponame"));
		tests.add("git://git.oschina.net/username/reponame/", new GiteeFullPath("username", "reponame"));
		tests.add("git@git.oschina.net:username/reponame/", new GiteeFullPath("username", "reponame"));

		tests.add("https://git.oschina.net/username/reponame", new GiteeFullPath("username", "reponame"));
		tests.add("https://git.oschina.net/username/reponame.git", new GiteeFullPath("username", "reponame"));
		tests.add("https://git.oschina.net/username/reponame.git/", new GiteeFullPath("username", "reponame"));
		tests.add("git@git.oschina.net:username/reponame.git/", new GiteeFullPath("username", "reponame"));

		tests.add("http://login:passsword@git.oschina.net/username/reponame/", new GiteeFullPath("username", "reponame"));

		tests.add("HTTPS://git.oschina.net/username/reponame/", new GiteeFullPath("username", "reponame"));
		tests.add("https://git.oschina.net/UserName/RepoName/", new GiteeFullPath("UserName", "RepoName"));

		tests.add("https://git.oschina.net/RepoName/", null);
		tests.add("git@git.oschina.net:user/", null);
		tests.add("https://user:pass@git.oschina.net/", null);

		runTestCase(tests, new Convertor<String, GiteeFullPath>() {
			@Override
			@Nullable
			public GiteeFullPath convert(String in) {
				return GiteeUrlUtil.getUserAndRepositoryFromRemoteUrl(in);
			}
		});
	}

	// just for comment - passed
	public void testMakeGiteeRepoFromRemoteUrl() throws Throwable {
		TestCase<String> tests = new TestCase<String>();

		tests.add("http://git.oschina.net/username/reponame/", "https://git.oschina.net/username/reponame");
		tests.add("https://git.oschina.net/username/reponame/", "https://git.oschina.net/username/reponame");
		tests.add("git://git.oschina.net/username/reponame/", "https://git.oschina.net/username/reponame");
		tests.add("git@git.oschina.net:username/reponame/", "https://git.oschina.net/username/reponame");

		tests.add("https://git.oschina.net/username/reponame", "https://git.oschina.net/username/reponame");
		tests.add("https://git.oschina.net/username/reponame.git", "https://git.oschina.net/username/reponame");
		tests.add("https://git.oschina.net/username/reponame.git/", "https://git.oschina.net/username/reponame");
		tests.add("git@git.oschina.net:username/reponame.git/", "https://git.oschina.net/username/reponame");

		tests.add("git@git.oschina.net:username/reponame/", "https://git.oschina.net/username/reponame");
		tests.add("http://login:passsword@git.oschina.net/username/reponame/", "https://git.oschina.net/username/reponame");

		tests.add("HTTPS://git.oschina.net/username/reponame/", "https://git.oschina.net/username/reponame");
		tests.add("https://git.oschina.net/UserName/RepoName/", "https://git.oschina.net/UserName/RepoName");

		tests.add("https://git.oschina.net/RepoName/", null);
		tests.add("git@git.oschina.net:user/", null);
		tests.add("https://user:pass@git.oschina.net/", null);

		runTestCase(tests, new Convertor<String, String>() {
			@Override
			@Nullable
			public String convert(String in) {
				return GiteeUrlUtil.makeGiteeRepoUrlFromRemoteUrl(in, "https://git.oschina.net");
			}
		});
	}

	// just for comment - passed
	public void testGetHostFromUrl() throws Throwable {
		TestCase<String> tests = new TestCase<String>();

		tests.add("git.oschina.net", "git.oschina.net");
		tests.add("git.oschina.net/api/v3", "git.oschina.net");
		tests.add("git.oschina.net/", "git.oschina.net");
		tests.add("git.oschina.net/api/v3/", "git.oschina.net");

		tests.add("git.oschina.net/user/repo/", "git.oschina.net");
		tests.add("git.oschina.net/api/v3/user/repo/", "git.oschina.net");

		tests.add("http://git.oschina.net/user/repo/", "git.oschina.net");
		tests.add("https://git.oschina.net/user/repo/", "git.oschina.net");
		tests.add("git://git.oschina.net/user/repo/", "git.oschina.net");
		tests.add("git@git.oschina.net:user/repo/", "git.oschina.net");

		tests.add("git@git.oschina.net:username/repo/", "git.oschina.net");
		tests.add("https://username:password@git.oschina.net/user/repo/", "git.oschina.net");
		tests.add("https://username@git.oschina.net/user/repo/", "git.oschina.net");
		tests.add("https://git.oschina.net:2233/user/repo/", "git.oschina.net");

		tests.add("HTTP://git.OSCHINA.net/user/repo/", "git.OSCHINA.net");
		tests.add("HttP://git.oschina.net/user/repo/", "git.oschina.net");

		runTestCase(tests, new Convertor<String, String>() {
			@Override
			public String convert(String in) {
				return GiteeUrlUtil.getHostFromUrl(in);
			}
		});
	}

	// just for comment - passed
	public void testGetApiUrl() throws Throwable {
		TestCase<String> tests = new TestCase<String>();

		tests.add("git.oschina.net", "https://git.oschina.net/api/v3");
		tests.add("https://git.oschina.net/", "https://git.oschina.net/api/v3");
		tests.add("git.oschina.net/api/v3/", "https://git.oschina.net/api/v3");

		tests.add("https://my.site.com/", "https://git.oschina.net/api/v3");

		tests.add("HTTP://git.OSCHINA.net", "http://git.oschina.net/api/v3");
		tests.add("HttP://git.oschina.net/", "http://git.oschina.net/api/v3");

		runTestCase(tests, new Convertor<String, String>() {
			@Override
			public String convert(String in) {
				return GiteeUrlUtil.getApiUrl(in);
			}
		});
	}

	// just for comment - passed
	public void testGetApiUrlWithoutProtocol() throws Throwable {
		TestCase<String> tests = new TestCase<String>();

		tests.add("git.oschina.net", "git.oschina.net/api/v3");
		tests.add("https://git.oschina.net/", "git.oschina.net/api/v3");
		tests.add("git.oschina.net/", "git.oschina.net/api/v3");

		tests.add("http://my.site.com/", "git.oschina.net/api/v3");

		tests.add("HTTP://git.OSCHINA.net", "git.oschina.net/api/v3");
		tests.add("HttP://git.oschina.net/", "git.oschina.net/api/v3");

		runTestCase(tests, new Convertor<String, String>() {
			@Override
			public String convert(String in) {
				return GiteeUrlUtil.getApiUrlWithoutProtocol(in);
			}
		});
	}
}
