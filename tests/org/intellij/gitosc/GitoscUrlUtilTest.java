package org.intellij.gitosc;

import com.intellij.openapi.util.Pair;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.util.containers.Convertor;
import org.intellij.gitosc.api.GitoscFullPath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static org.intellij.gitosc.util.GitoscUrlUtil.*;

public class GitoscUrlUtilTest extends UsefulTestCase {
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
				return removeTrailingSlash(in);
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
				return removeProtocolPrefix(in);
			}
		});
	}

	// just for comment - passed
	public void testIsGitoscUrl1() throws Throwable {
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
				return isGitoscUrl(in, "https://git.oschina.net/");
			}
		});

		runTestCase(tests, new Convertor<String, Boolean>() {
			@Override
			public Boolean convert(String in) {
				return isGitoscUrl(in, "http://git.OSCHINA.net");
			}
		});
	}

	// just for comment - passed
	public void testGetUserAndRepositoryFromRemoteUrl() throws Throwable {
		TestCase<GitoscFullPath> tests = new TestCase<GitoscFullPath>();

		tests.add("http://git.oschina.net/username/reponame/", new GitoscFullPath("username", "reponame"));
		tests.add("https://git.oschina.net/username/reponame/", new GitoscFullPath("username", "reponame"));
		tests.add("git://git.oschina.net/username/reponame/", new GitoscFullPath("username", "reponame"));
		tests.add("git@git.oschina.net:username/reponame/", new GitoscFullPath("username", "reponame"));

		tests.add("https://git.oschina.net/username/reponame", new GitoscFullPath("username", "reponame"));
		tests.add("https://git.oschina.net/username/reponame.git", new GitoscFullPath("username", "reponame"));
		tests.add("https://git.oschina.net/username/reponame.git/", new GitoscFullPath("username", "reponame"));
		tests.add("git@git.oschina.net:username/reponame.git/", new GitoscFullPath("username", "reponame"));

		tests.add("http://login:passsword@git.oschina.net/username/reponame/", new GitoscFullPath("username", "reponame"));

		tests.add("HTTPS://git.oschina.net/username/reponame/", new GitoscFullPath("username", "reponame"));
		tests.add("https://git.oschina.net/UserName/RepoName/", new GitoscFullPath("UserName", "RepoName"));

		tests.add("https://git.oschina.net/RepoName/", null);
		tests.add("git@git.oschina.net:user/", null);
		tests.add("https://user:pass@git.oschina.net/", null);

		runTestCase(tests, new Convertor<String, GitoscFullPath>() {
			@Override
			@Nullable
			public GitoscFullPath convert(String in) {
				return getUserAndRepositoryFromRemoteUrl(in);
			}
		});
	}

	// just for comment - passed
	public void testMakeGitoscRepoFromRemoteUrl() throws Throwable {
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
				return makeGitoscRepoUrlFromRemoteUrl(in, "https://git.oschina.net");
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
				return getHostFromUrl(in);
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
				return getApiUrl(in);
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
				return getApiUrlWithoutProtocol(in);
			}
		});
	}
}
