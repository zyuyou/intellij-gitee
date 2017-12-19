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

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;
import org.intellij.gitosc.api.data.*;
import org.intellij.gitosc.api.GitoscConnection.PagedRequest;
import org.intellij.gitosc.api.GitoscConnection.ArrayPagedRequest;
import org.intellij.gitosc.api.requests.GitoscChangeIssueStateRequest;
import org.intellij.gitosc.api.requests.GitoscGistRequest;
import org.intellij.gitosc.api.requests.GitoscRepoRequest;
import org.intellij.gitosc.exceptions.GitoscAuthenticationException;
import org.intellij.gitosc.exceptions.GitoscConfusingException;
import org.intellij.gitosc.exceptions.GitoscJsonException;
import org.intellij.gitosc.exceptions.GitoscStatusCodeException;
import org.intellij.gitosc.util.GitoscAuthData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;

import static org.intellij.gitosc.GitoscConstants.*;

/**
 * @author Yuyou Chow
 *
 * Based on https://github.com/JetBrains/intellij-community/blob/master/plugins/github/src/org/jetbrains/plugins/github/api/GithubApiUtil.java
 * @author JetBrains s.r.o.
 */
public class GitoscApiUtil {
	@NotNull private static final Gson gson = initGson();

	private static final String PER_PAGE = "per_page=100";

	private static final Header ACCEPT_V3_JSON_HTML_MARKUP = new BasicHeader("Accept", "application/vnd.github.v3.html+json");
	private static final Header ACCEPT_V3_JSON = new BasicHeader("Accept", "application/vnd.github.v3+json");

	private static Gson initGson(){
		GsonBuilder builder = new GsonBuilder();
		builder.setDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		builder.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES);
		return builder.create();
	}

	@NotNull
	public static <T> T fromJson(@Nullable JsonElement json, @NotNull Class<T> classT) throws IOException {
		if (json == null) {
			throw new GitoscJsonException("Unexpected empty response");
		}

		T res;
		try {
			//cast as workaround for early java 1.6 bug
			//noinspection RedundantCast
			res = (T)gson.fromJson(json, classT);
		}
		catch (ClassCastException | JsonParseException e) {
			throw new GitoscJsonException("Parse exception while converting JSON to object " + classT.toString(), e);
		}
		if (res == null) {
			throw new GitoscJsonException("Empty Json response");
		}
		return res;
	}

	@NotNull
	public static String getScopedToken(@NotNull GitoscConnection connection, @NotNull Collection<String> scopes, @NotNull String note)
		throws IOException {
		try {
			return getNewScopedToken(connection, scopes, note).getAccessToken();
		}
		catch (GitoscStatusCodeException e) {
			if (e.getError() != null) {
				e.printStackTrace();
			}
			throw e;
		}
	}

	@NotNull
	private static GitoscAuthorization getNewScopedToken(@NotNull GitoscConnection connection,
	                                                     @NotNull Collection<String> scopes,
	                                                     @NotNull String note)
		throws IOException {

		try {
			GitoscAuthData.SessionAuth sessionAuth = connection.getAuth().getSessionAuth();
			assert sessionAuth != null && !"".equals(sessionAuth.getPassword());

			String requestBody = JOINER.join(
				AUTH_GRANT_TYPE, AUTH_CLIENT_ID, AUTH_CLIENT_SECRET,
				"scope=" + SPACE_JOINER.join(scopes),
				"username=" + sessionAuth.getLogin(),
				"password=" + sessionAuth.getPassword()
			);
			return loadPost(connection, "/oauth/token", requestBody, GitoscAuthorization.class);
		}
		catch (GitoscConfusingException e) {
			e.setDetails("Can't create token: scopes - " + scopes + " - note " + note);
			throw e;
		}
	}

	@NotNull
	public static String getMasterToken(@NotNull GitoscConnection connection, @NotNull String note) throws IOException {
		// "projects" - read/write access to public/private repositories
		// "gists" - create/delete gists
		List<String> scopes = Arrays.asList("projects", "gists", "user_info");

		return getScopedToken(connection, scopes, note);
	}

	@NotNull
	public static String getTasksToken(@NotNull GitoscConnection connection,
	                                   @NotNull String user,
	                                   @NotNull String repo,
	                                   @NotNull String note) throws IOException {

		List<String> scopes = Arrays.asList("projects", "issues", "user_info");
		return getScopedToken(connection, scopes, note);
	}

	//====================================================================================
	// gitee.com/api/v5 - method calls
	//====================================================================================
	public static void deleteGist(@NotNull GitoscConnection connection, @NotNull String id) throws IOException {
		try {
			String path = "/gists/" + id;
			connection.deleteRequest(path);
		}
		catch (GitoscConfusingException e) {
			e.setDetails("Can't delete gist: id - " + id);
			throw e;
		}
	}

	@NotNull
	public static GitoscGist getGist(@NotNull GitoscConnection connection, @NotNull String id) throws IOException {
		try {
			String path = "/gists/" + id;
			return load(connection, path, GitoscGist.class, ACCEPT_V3_JSON);
		}
		catch (GitoscConfusingException e) {
			e.setDetails("Can't get gist info: id " + id);
			throw e;
		}
	}

	@NotNull
	public static GitoscGist createGist(@NotNull GitoscConnection connection,
	                                    @NotNull List<GitoscGistRequest.FileContent> contents,
	                                    @NotNull String description,
	                                    boolean isPrivate) throws IOException {
		try {
			GitoscGistRequest request = new GitoscGistRequest(contents, description, !isPrivate);
			return post(connection, "/gists", request, GitoscGist.class, ACCEPT_V3_JSON);
		}
		catch (GitoscConfusingException e) {
			e.setDetails("Can't create gist");
			throw e;
		}
	}

	@NotNull
	public static List<GitoscIssue> getIssuesQueried(@NotNull GitoscConnection connection,
	                                                 @NotNull String user,
	                                                 @NotNull String repo,
	                                                 @Nullable String assignedUser,
	                                                 @Nullable String query,
	                                                 boolean withClosed) throws IOException {
		try {
			String state = withClosed ? "" : " state:open";
			String assignee = StringUtil.isEmptyOrSpaces(assignedUser) ? "" : " assignee:" + assignedUser;
			query = StringUtil.isEmptyOrSpaces(query) ? "" : query;
			query = URLEncoder.encode(query + "+repo:" + user + "/" + repo + state + assignee, CharsetToolkit.UTF8);
			String path = "/search/issues?q=" + query;

			//TODO: Use bodyHtml for issues - GitHub does not support this feature for SearchApi yet
			return loadAll(connection, path, GitoscIssue[].class, ACCEPT_V3_JSON);
		}
		catch (GitoscConfusingException e) {
			e.setDetails("Can't get queried issues: " + user + "/" + repo + " - " + query);
			throw e;
		}
	}

	/*
   * Open issues only
   */
	@NotNull
	public static List<GitoscIssue> getIssuesAssigned(@NotNull GitoscConnection connection,
	                                                  @NotNull String user,
	                                                  @NotNull String repo,
	                                                  @Nullable String assigned,
	                                                  int max,
	                                                  boolean withClosed) throws IOException {
		try {
			String state = "state=" + (withClosed ? "all" : "open");
			String path;

			if (StringUtil.isEmptyOrSpaces(assigned)) {
				path = "/repos/" + user + "/" + repo + "/issues?" + PER_PAGE + "&" + state;
			}else {
				path = "/repos/" + user + "/" + repo + "/issues?assignee=" + assigned + "&" + PER_PAGE + "&" + state;
			}

			PagedRequest<GitoscIssue> request = new ArrayPagedRequest<>(path, GitoscIssue[].class, ACCEPT_V3_JSON);

			List<GitoscIssue> result = new ArrayList<>();
			while (request.hasNext() && max > result.size()) {
				result.addAll(request.next(connection));
			}
			return result;
		}
		catch (GitoscConfusingException e) {
			e.setDetails("Can't get assigned issues: " + user + "/" + repo + " - " + assigned);
			throw e;
		}
	}

	@NotNull
	public static List<GitoscIssueComment> getIssueComments(@NotNull GitoscConnection connection,
	                                                        @NotNull String user,
	                                                        @NotNull String repo,
	                                                        String id)
		throws IOException {
		try {
			String path = "/repos/" + user + "/" + repo + "/issues/" + id + "/comments?" + PER_PAGE;
			return loadAll(connection, path, GitoscIssueComment[].class, ACCEPT_V3_JSON);
		}
		catch (GitoscConfusingException e) {
			e.setDetails("Can't get issue comments: " + user + "/" + repo + " - " + id);
			throw e;
		}
	}

	@NotNull
	public static GitoscIssue getIssue(@NotNull GitoscConnection connection, @NotNull String user, @NotNull String repo, @NotNull String id)
		throws IOException {
		try {
			String path = "/repos/" + user + "/" + repo + "/issues/" + id;
			return load(connection, path, GitoscIssue.class, ACCEPT_V3_JSON);
		}
		catch (GitoscConfusingException e) {
			e.setDetails("Can't get issue info: " + user + "/" + repo + " - " + id);
			throw e;
		}
	}

	public static void setIssueState(@NotNull GitoscConnection connection,
	                                 @NotNull String user,
	                                 @NotNull String repo,
	                                 @NotNull String id,
	                                 @NotNull String title,
	                                 boolean open)
		throws IOException {
		try {
			String path = "/repos/" + user + "/" + repo + "/issues/" + id;

			GitoscChangeIssueStateRequest request = new GitoscChangeIssueStateRequest(open ? "open" : "closed", title);
			JsonElement result = connection.patchRequest(path, gson.toJson(request), ACCEPT_V3_JSON);
			fromJson(result, GitoscIssue.class);
		}
		catch (GitoscConfusingException e) {
			e.setDetails("Can't set issue state: " + user + "/" + repo + " - " + id + "@" + (open ? "open" : "closed"));
			throw e;
		}
	}

	@NotNull
	public static GitoscUserDetailed getCurrentUser(@NotNull GitoscConnection connection) throws IOException {
		try {
			String requestBody;

			switch (connection.getAuth().getAuthType()){
				case TOKEN:
					return load(connection, "/user", GitoscUserDetailed.class);
				default:
					GitoscAuthData.SessionAuth sessionAuth = connection.getAuth().getSessionAuth();
					assert sessionAuth != null;
					requestBody = JOINER.join("email=" + sessionAuth.getLogin(), "password=" + sessionAuth.getPassword());
					return loadPost(connection, "/session", requestBody, GitoscUserDetailed.class);
			}
		}
		catch (GitoscConfusingException e) {
			e.setDetails("Can't get user info");
			throw e;
		}
	}

	@NotNull
	private static <T> List<T> loadAll(@NotNull GitoscConnection connection,
	                                   @NotNull String path,
	                                   @NotNull Class<? extends T[]> type,
	                                   @NotNull Header... headers) throws IOException {
		PagedRequest<T> request = new GitoscConnection.ArrayPagedRequest<>(path, type, headers);
		return request.getAll(connection);
	}

	@NotNull
	private static <T> T load(@NotNull GitoscConnection connection,
	                          @NotNull String path,
	                          @NotNull Class<? extends T> type,
	                          @NotNull Header... headers) throws IOException {
		JsonElement result = connection.getRequest(path, headers);
		return fromJson(result, type);
	}

	@NotNull
	private static <T> T post(@NotNull GitoscConnection connection,
	                          @NotNull String path,
	                          @NotNull Object request,
	                          @NotNull Class<? extends T> type,
	                          @NotNull Header... headers) throws IOException {

		JsonElement result = connection.postRequest(path, gson.toJson(request), headers);
		return fromJson(result, type);
	}

	@NotNull
	private static <T> T loadPost(@NotNull GitoscConnection connection,
	                              @NotNull String path,
	                              @Nullable String requestBody,
	                              @NotNull Class<? extends T> type,
	                              @NotNull Header... headers) throws IOException {

		JsonElement result = connection.postRequest(path, requestBody, headers);
		return fromJson(result, type);
	}

	@NotNull
	public static GitoscAuthorization getAuthorization(@NotNull GitoscConnection connection) throws IOException {
		try {
			if(connection.getAuth().getAuthType() != GitoscAuthData.AuthType.SESSION) {
				throw new GitoscAuthenticationException("Get Authorization AuthType Error: " + connection.getAuth().getAuthType());
			}

			GitoscAuthData.SessionAuth sessionAuth = connection.getAuth().getSessionAuth();
			assert sessionAuth != null;

			String requestBody = JOINER.join(
				AUTH_GRANT_TYPE, AUTH_CLIENT_ID,AUTH_CLIENT_SECRET,
				"username=" + sessionAuth.getLogin(),
				"password=" + sessionAuth.getPassword()
			);
			return loadPost(connection, "/oauth/token", requestBody, GitoscAuthorization.class);
		}
		catch (GitoscAuthenticationException e){
			e.printStackTrace();
			throw e;
		}
		catch (GitoscConfusingException e) {
			e.setDetails("Can't get user info");
			throw e;
		}
	}

	//====================================================================================
	// gitee.com/api/v3 - method calls
	//====================================================================================

	@NotNull
	public static List<GitoscRepo> getAvailableRepos(@NotNull GitoscConnection connection) throws IOException {
		try{
			List<GitoscRepo> repos = new ArrayList<GitoscRepo>();
			repos.addAll(getUserRepos(connection, true));
			return repos;
		}catch (GitoscConfusingException e){
			e.setDetails("Can't get available repositories");
			throw e;
		}
	}

	@NotNull
	public static GitoscUserDetailed getCurrentUserDetailed(@NotNull GitoscConnection connection) throws IOException {
		try {
			String requestBody;

			switch (connection.getAuth().getAuthType()){
				case TOKEN:
					return load(connection, "/user", GitoscUserDetailed.class);
				default:
					GitoscAuthData.SessionAuth sessionAuth = connection.getAuth().getSessionAuth();
					assert sessionAuth != null;
					requestBody = JOINER.join("email=" + sessionAuth.getLogin(), "password=" + sessionAuth.getPassword());
					return loadPost(connection, "/session", requestBody, GitoscUserDetailed.class);
			}
		}
		catch (GitoscConfusingException e) {
			e.setDetails("Can't get user info");
			throw e;
		}
	}

	@NotNull
	public static GitoscRepoDetailed getDetailedRepoInfo(@NotNull GitoscConnection connection, @NotNull String owner, @NotNull String name)
		throws IOException {
		try {
			switch (connection.getAuth().getAuthType()){
				case TOKEN:
					final String request = "/repos/" + owner + "/" + name;
					return load(connection, request, GitoscRepoDetailed.class, ACCEPT_V3_JSON);
				default:
					final String path = "/projects/" + owner + "%2F" + name;
					return load(connection, path, GitoscRepoDetailed.class, ACCEPT_V3_JSON);
			}
		}
		catch (GitoscConfusingException e) {
			e.setDetails("Can't get repository info: " + owner + "/" + name);
			throw e;
		}
	}

	@NotNull
	public static List<GitoscRepo> getUserRepos(@NotNull GitoscConnection connection) throws IOException {
		return getUserRepos(connection, false);
	}

	@NotNull
	public static List<GitoscRepo> getUserRepos(@NotNull GitoscConnection connection, boolean allAssociated) throws IOException {
		try {
			String path;

			switch (connection.getAuth().getAuthType()){
				case TOKEN:
					String type = allAssociated ? "" : "type=owner&";
					path = "/user/repos?" + type + PER_PAGE;
					return loadAll(connection, path, GitoscRepo[].class, ACCEPT_V3_JSON);
				default:
					path = "/projects?";
					return loadAll(connection, path, GitoscRepo[].class, ACCEPT_V3_JSON);
//					GitoscConnection.PagedRequest<GitoscRepo> request = new GitoscConnection.PagedRequest<GitoscRepo>(path, GitoscRepo.class, GitoscRepoRaw[].class);
//					return request.getAll(connection);
			}
		}
		catch (GitoscConfusingException e) {
			e.setDetails("Can't get user repositories");
			throw e;
		}
	}

	@NotNull
	public static GitoscRepo createRepo(@NotNull GitoscConnection connection,
	                                    @NotNull String name,
	                                    @NotNull String description,
	                                    boolean isPrivate)
		throws IOException {

		try {
			String path;
			switch (connection.getAuth().getAuthType()) {
				case TOKEN:
					path = "/user/repos";
					GitoscRepoRequest request = new GitoscRepoRequest(name, description, isPrivate);
					assert connection.getAuth().getTokenAuth() != null;
					request.setAccessToken(connection.getAuth().getTokenAuth().getToken());
					return post(connection, path, request, GitoscRepo.class, ACCEPT_V3_JSON);
				default:
					path = "/projects";
					String requestBody = JOINER.join("name=" + name, "description=" + description, "private=" + (isPrivate? 1 : 0));
					return loadPost(connection, path, requestBody, GitoscRepo.class, ACCEPT_V3_JSON);
//					return createDataFromRaw(fromJson(connection.postRequest(path, requestBody), GitoscRepoRaw.class), GitoscRepo.class);
			}
		}
		catch (GitoscConfusingException e) {
			e.setDetails("Can't create repository: " + name);
			throw e;
		}
	}

}
